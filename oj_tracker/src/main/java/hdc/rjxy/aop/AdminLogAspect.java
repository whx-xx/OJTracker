package hdc.rjxy.aop;

import hdc.rjxy.mapper.UserMapper;
import hdc.rjxy.pojo.AdminOpLog;
import hdc.rjxy.pojo.User;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.dto.ChangePasswordReq;
import hdc.rjxy.pojo.dto.UpdateNicknameReq;
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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AdminLogAspect {

    @Autowired
    private AdminOpLogService adminOpLogService;

    @Autowired
    private UserMapper userMapper;

    @Around("@annotation(logAnno)")
    public Object around(ProceedingJoinPoint point, LogAdminOp logAnno) throws Throwable {
        Object result = null;
        Exception ex = null;
        String oldData = null; // 用于存储旧数据（如旧昵称）

        try {
            // 1. 预处理：如果是修改昵称操作，先查出旧昵称
            oldData = preHandleOldData(point);

            // 2. 执行目标方法
            result = point.proceed();
            return result;
        } catch (Exception e) {
            ex = e;
            throw e;
        } finally {
            // 3. 记录日志 (传入 result 以便解析返回值中的状态)
            saveLog(point, logAnno, ex, oldData, result);
        }
    }

    /**
     * 预处理：获取旧数据
     */
    private String preHandleOldData(ProceedingJoinPoint point) {
        try {
            Object[] args = point.getArgs();
            if (args == null) return null;

            // 判断是否包含修改昵称的 DTO
            boolean isNicknameUpdate = false;
            for (Object arg : args) {
                if (arg instanceof UpdateNicknameReq) {
                    isNicknameUpdate = true;
                    break;
                }
            }

            if (isNicknameUpdate) {
                Long targetUserId = resolveTargetUserId(point);
                if (targetUserId != null) {
                    User user = userMapper.selectById(targetUserId);
                    return user != null ? user.getNickname() : null;
                }
            }
        } catch (Exception e) {
            log.warn("AOP预获取旧数据失败", e);
        }
        return null;
    }

    private void saveLog(ProceedingJoinPoint point, LogAdminOp logAnno, Exception ex, String oldData, Object result) {
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
            Long targetUserId = resolveTargetUserId(point);
            opLog.setTargetUserId(targetUserId);

            // 2. 设置备注
            if (ex != null) {
                opLog.setRemark("操作失败: " + ex.getMessage());
            } else {
                // 传入 oldData 和 result 进行解析
                String detail = resolveDetailRemark(point, oldData, result);
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

                if (argValue instanceof Long) {
                    PathVariable pathVar = param.getAnnotation(PathVariable.class);
                    if (pathVar != null) {
                        return (Long) argValue;
                    }
                    String name = param.getName();
                    if ("userId".equals(name) || "id".equals(name)) {
                        return (Long) argValue;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析操作日志目标ID异常", e);
        }
        return null;
    }

    /**
     * 解析操作详情
     */
    private String resolveDetailRemark(ProceedingJoinPoint point, String oldData, Object result) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            String methodName = signature.getName();

            // --- 新增逻辑：切换定时任务状态 ---
            if ("toggleSchedule".equals(methodName)) {
                if (result != null) {
                    try {
                        // 使用反射获取 R 对象的 data 字段 (假设返回类型为 R<Boolean>)
                        // 这样可以避免直接依赖 R 类可能带来的导入问题
                        Method getDataMethod = result.getClass().getMethod("getData");
                        Object data = getDataMethod.invoke(result);
                        if (data instanceof Boolean) {
                            return ((Boolean) data) ? "动作: 启动定时任务" : "动作: 禁用定时任务";
                        }
                    } catch (Exception e) {
                        // 忽略反射异常，返回默认值
                        log.debug("解析定时任务状态返回值失败", e);
                    }
                }
                return "动作: 切换定时任务状态";
            }
            // --------------------------------

            if ("resetPassword".equals(methodName)) {
                return "动作: 重置为默认密码";
            }
            Parameter[] parameters = signature.getMethod().getParameters();
            Object[] args = point.getArgs();

            if (args == null || parameters == null) return "";

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < parameters.length; i++) {
                if (i >= args.length) break;

                Object arg = args[i];
                Parameter param = parameters[i];

                String paramName = param.getName();
                RequestParam requestParam = param.getAnnotation(RequestParam.class);
                if (requestParam != null) {
                    if (!requestParam.value().isEmpty()) {
                        paramName = requestParam.value();
                    } else if (!requestParam.name().isEmpty()) {
                        paramName = requestParam.name();
                    }
                }

                if (arg instanceof UpdateStatusReq) {
                    UpdateStatusReq req = (UpdateStatusReq) arg;
                    if (req.getStatus() != null) {
                        return req.getStatus() == 1 ? "动作: 解封" : "动作: 封禁";
                    }
                }

                if (arg instanceof UpdateNicknameReq) {
                    UpdateNicknameReq req = (UpdateNicknameReq) arg;
                    String oldStr = (oldData == null) ? "未知" : oldData;
                    return "原昵称: " + oldStr + " -> 新昵称: " + req.getNickname();
                }

                if ("jobType".equals(paramName) && arg instanceof String) {
                    sb.append("类型: ").append(arg);
                }
                if ("days".equals(paramName) && arg instanceof Integer) {
                    sb.append(", 天数: ").append(arg);
                }
                if ("jobId".equals(paramName) && arg instanceof Long) {
                    if ("rerun".equals(signature.getName())) {
                        sb.append("重跑JobId: ").append(arg);
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