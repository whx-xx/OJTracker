package hdc.rjxy.aop;

import hdc.rjxy.pojo.AdminOpLog;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.dto.UpdateStatusReq;
import hdc.rjxy.service.AdminOpLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AdminLogAspect {

    @Autowired
    private AdminOpLogService adminOpLogService;

    @Around("@annotation(logAnno)")
    public Object around(ProceedingJoinPoint point, LogAdminOp logAnno) throws Throwable {
        Object result = null;
        Exception ex = null;

        try {
            result = point.proceed();
            return result;
        } catch (Exception e) {
            ex = e;
            throw e;
        } finally {
            saveLog(point, logAnno, ex);
        }
    }

    private void saveLog(ProceedingJoinPoint point, LogAdminOp logAnno, Exception ex) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = (attributes != null) ? attributes.getRequest() : null;
            if (request == null) return;

            HttpSession session = request.getSession(false);
            UserSession currentUser = (session != null) ? (UserSession) session.getAttribute("user") : null;
            if (currentUser == null) return;

            AdminOpLog opLog = new AdminOpLog();
            opLog.setAdminId(currentUser.getId());
            opLog.setOpType(logAnno.value());
            opLog.setOpTime(LocalDateTime.now());

            // 1. 解析目标用户 ID
            // 关键修复：如果没有找到目标用户，必须返回 null，而不是 0L。
            // 因为 0L 会触发外键约束错误 (Foreign key constraint fails)，而 null 被允许。
            Long targetUserId = resolveTargetUserId(point);
            opLog.setTargetUserId(targetUserId);

            // 2. 设置备注
            if (ex != null) {
                opLog.setRemark("操作失败: " + ex.getMessage());
            } else {
                String detail = resolveDetailRemark(point);
                // 如果解析出了详情，就拼接到日志中
                opLog.setRemark(detail.isEmpty() ? "" : detail);
            }

            adminOpLogService.saveLog(opLog);

        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    /**
     * 智能解析 targetUserId
     */
    private Long resolveTargetUserId(ProceedingJoinPoint point) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Parameter[] parameters = signature.getMethod().getParameters();
            Object[] args = point.getArgs();

            if (parameters == null || args == null) return null;

            for (int i = 0; i < parameters.length; i++) {
                if (i >= args.length) break;

                Parameter param = parameters[i];
                Object argValue = args[i];

                // 只关心 Long 类型的参数 (用户ID)
                if (argValue instanceof Long) {
                    // 策略A：检查 @PathVariable 注解
                    PathVariable pathVar = param.getAnnotation(PathVariable.class);
                    if (pathVar != null) {
                        return (Long) argValue;
                    }
                    // 策略B：检查参数名
                    String name = param.getName();
                    if ("userId".equals(name) || "id".equals(name)) {
                        return (Long) argValue;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析操作日志目标ID异常", e);
        }
        // 关键修复：返回 null 而不是 0L
        return null;
    }

    /**
     * 解析操作详情（支持 UpdateStatusReq 和 Sync 任务）
     * 改进版：优先使用 @RequestParam 注解获取参数名，解决编译后参数名丢失导致 remark 为空的问题
     */
    private String resolveDetailRemark(ProceedingJoinPoint point) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            // 使用 Java 反射获取参数对象，以便获取注解
            Parameter[] parameters = signature.getMethod().getParameters();
            Object[] args = point.getArgs();

            if (args == null || parameters == null) return "";

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < parameters.length; i++) {
                if (i >= args.length) break;

                Object arg = args[i];
                Parameter param = parameters[i];

                // 1. 获取参数名：优先取 @RequestParam 的 value/name，其次取变量名
                String paramName = param.getName();
                RequestParam requestParam = param.getAnnotation(RequestParam.class);
                if (requestParam != null) {
                    if (!requestParam.value().isEmpty()) {
                        paramName = requestParam.value();
                    } else if (!requestParam.name().isEmpty()) {
                        paramName = requestParam.name();
                    }
                }

                // 2. 处理同步任务 (AdminSyncController.run)
                // 匹配 jobType (String)
                if ("jobType".equals(paramName) && arg instanceof String) {
                    sb.append("类型: ").append(arg);
                }
                // 匹配 days (Integer)
                if ("days".equals(paramName) && arg instanceof Integer) {
                    sb.append(", 天数: ").append(arg);
                }

                // 3. 处理重跑任务 (AdminSyncController.rerun)
                // 匹配 jobId (Long)
                if ("jobId".equals(paramName) && arg instanceof Long) {
                    if ("rerun".equals(signature.getName())) {
                        sb.append("重跑JobId: ").append(arg);
                    }
                }

                // 4. 处理 UpdateStatusReq (用户状态变更)
                if (arg instanceof UpdateStatusReq) {
                    UpdateStatusReq req = (UpdateStatusReq) arg;
                    if (req.getStatus() != null) {
                        return req.getStatus() == 1 ? "动作: 解封" : "动作: 封禁";
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("解析操作日志详情异常", e);
            return "";
        }
    }

}