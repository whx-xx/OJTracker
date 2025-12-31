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

            // 1. 解析目标用户 ID (增加兜底逻辑，防止数据库报错)
            Long targetUserId = resolveTargetUserId(point);
            opLog.setTargetUserId(targetUserId != null ? targetUserId : 0L);

            // 2. 设置备注
            if (ex != null) {
                opLog.setRemark("操作失败: " + ex.getMessage());
            } else {
                String detail = resolveDetailRemark(point);
                opLog.setRemark("操作成功" + (detail.isEmpty() ? "" : ": " + detail));
            }

            adminOpLogService.saveLog(opLog);

        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    /**
     * 智能解析 targetUserId
     * 策略：
     * 1. 寻找带有 @PathVariable 的 Long 类型参数 (最准确)
     * 2. 寻找参数名为 userId 或 id 的 Long 类型参数
     */
    private Long resolveTargetUserId(ProceedingJoinPoint point) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            // 获取方法的参数元数据 (包含注解信息)
            Parameter[] parameters = signature.getMethod().getParameters();
            Object[] args = point.getArgs();

            if (parameters == null || args == null) return 0L;

            for (int i = 0; i < parameters.length; i++) {
                if (i >= args.length) break;

                Parameter param = parameters[i];
                Object argValue = args[i];

                // 只关心 Long 类型的参数 (用户ID)
                if (argValue instanceof Long) {

                    // 策略A：检查 @PathVariable 注解 (最稳健，不受编译参数影响)
                    PathVariable pathVar = param.getAnnotation(PathVariable.class);
                    if (pathVar != null) {
                        // 如果是 Long 类型的 PathVariable，99% 是 userId
                        return (Long) argValue;
                    }

                    // 策略B：检查参数名 (作为补充)
                    String name = param.getName();
                    if ("userId".equals(name) || "id".equals(name)) {
                        return (Long) argValue;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析操作日志目标ID异常", e);
        }
        return 0L; // 默认返回 0，满足数据库 NOT NULL 约束
    }

    private String resolveDetailRemark(ProceedingJoinPoint point) {
        Object[] args = point.getArgs();
        if (args == null) return "";

        for (Object arg : args) {
            if (arg instanceof UpdateStatusReq) {
                UpdateStatusReq req = (UpdateStatusReq) arg;
                if (req.getStatus() != null) {
                    return req.getStatus() == 1 ? "启用用户" : "禁用用户";
                }
            }
            // 你可以在这里继续解析 UpdateNicknameReq 等其他参数
        }
        return "";
    }
}