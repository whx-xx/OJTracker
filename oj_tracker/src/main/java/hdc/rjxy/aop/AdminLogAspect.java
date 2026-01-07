package hdc.rjxy.aop;

import hdc.rjxy.mapper.UserMapper;
import hdc.rjxy.pojo.AdminOpLog;
import hdc.rjxy.pojo.User;
import hdc.rjxy.pojo.UserSession;
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

/**
 * 管理员操作日志切面类
 * 作用：拦截带有 @LogAdminOp 注解的方法，自动记录管理员的操作行为
 */
@Aspect     // 声明这是一个切面类
@Component  // 将该类交给 Spring 容器管理
@Slf4j      // 开启日志功能
public class AdminLogAspect {

    @Autowired
    private AdminOpLogService adminOpLogService; // 注入日志服务，用于将日志保存到数据库

    @Autowired
    private UserMapper userMapper; // 注入 UserMapper，用于查询修改前的旧数据

    /**
     * 环绕通知 (Around Advice)
     * 核心逻辑：在目标方法执行前后进行拦截，实现“操作前获取旧数据”和“操作后记录日志”
     *
     * @param point   连接点，代表被拦截的方法
     * @param logAnno 方法上的注解对象，包含了操作描述（如"修改用户"）
     * @return 目标方法的执行结果
     */
    @Around("@annotation(logAnno)")
    public Object around(ProceedingJoinPoint point, LogAdminOp logAnno) throws Throwable {
        Object result = null;
        Exception ex = null;
        String oldData = null; // 用于存储旧数据（如修改前的昵称）

        try {
            // ==================== 1. 前置处理 ====================
            // 在目标方法执行前，先尝试去数据库查询旧数据
            // 例如：如果要把“张三”改为“李四”，这里会先查出“张三”存起来
            oldData = preHandleOldData(point);

            // ==================== 2. 执行目标方法 ====================
            // 这一步是真正执行 Controller 中的业务逻辑
            result = point.proceed();

            return result;
        } catch (Exception e) {
            // 如果业务逻辑抛出异常，捕获它以便记录到日志中（如“操作失败：xxx”）
            ex = e;
            throw e; // 必须将异常继续抛出，否则全局异常处理器捕获不到，前端会认为请求成功
        } finally {
            // ==================== 3. 后置处理 (最终通知) ====================
            // 无论方法执行成功还是失败，都要记录操作日志
            // 传入 result 是为了能够解析业务方法的返回值（例如判断定时任务开启还是关闭）
            saveLog(point, logAnno, ex, oldData, result);
        }
    }

    /**
     * 辅助方法：预处理获取旧数据
     * 目前主要用于“修改昵称”场景，获取修改前的原昵称
     */
    private String preHandleOldData(ProceedingJoinPoint point) {
        try {
            Object[] args = point.getArgs(); // 获取方法参数
            if (args == null) return null;

            // 遍历参数，判断是否包含修改昵称的请求对象 (UpdateNicknameReq)
            boolean isNicknameUpdate = false;
            for (Object arg : args) {
                if (arg instanceof UpdateNicknameReq) {
                    isNicknameUpdate = true;
                    break;
                }
            }

            // 如果是修改昵称的操作
            if (isNicknameUpdate) {
                // 1. 解析出目标用户的 ID
                Long targetUserId = resolveTargetUserId(point);
                // 2. 查询数据库获取当前（旧）昵称
                if (targetUserId != null) {
                    User user = userMapper.selectById(targetUserId);
                    return user != null ? user.getNickname() : null;
                }
            }
        } catch (Exception e) {
            // 记录日志时不应该影响主业务流程，所以这里只打印警告，不抛出异常
            log.warn("AOP预获取旧数据失败", e);
        }
        return null;
    }

    /**
     * 核心逻辑：构建并保存日志对象
     *
     * @param point   连接点
     * @param logAnno 注解信息
     * @param ex      异常信息（如果不为 null 说明操作失败）
     * @param oldData 旧数据
     * @param result  方法返回值
     */
    private void saveLog(ProceedingJoinPoint point, LogAdminOp logAnno, Exception ex, String oldData, Object result) {
        try {
            // 1. 获取当前的 HttpServletRequest
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = (attributes != null) ? attributes.getRequest() : null;
            if (request == null) return;

            // 2. 获取当前登录的管理员信息 (从 Session 中)
            HttpSession session = request.getSession(false);
            UserSession currentUser = (session != null) ? (UserSession) session.getAttribute("user") : null;
            if (currentUser == null) return; // 如果没登录（理论上 AuthInterceptor 会拦截，这里做防御性编程）

            // 3. 构建日志实体对象
            AdminOpLog opLog = new AdminOpLog();
            opLog.setAdminId(currentUser.getId());  // 操作人 ID
            opLog.setOpType(logAnno.value());       // 操作类型（来自注解）
            opLog.setOpTime(LocalDateTime.now());   // 操作时间

            // 4. 解析目标用户 ID (即管理员是在操作谁)
            Long targetUserId = resolveTargetUserId(point);
            opLog.setTargetUserId(targetUserId);

            // 5. 设置备注信息 (详细描述)
            if (ex != null) {
                opLog.setRemark("操作失败: " + ex.getMessage());
            } else {
                // 如果成功，根据参数、旧数据和返回值，生成智能的文字描述
                String detail = resolveDetailRemark(point, oldData, result);
                opLog.setRemark(detail.isEmpty() ? "" : detail);
            }

            // 6. 保存到数据库
            adminOpLogService.saveLog(opLog);

        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    /**
     * 智能解析目标用户 ID (targetUserId)
     * 策略：自动寻找方法参数中名为 userId, id 的参数，或者带有 @PathVariable 的 Long 类型参数
     */
    private Long resolveTargetUserId(ProceedingJoinPoint point) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Parameter[] parameters = signature.getMethod().getParameters(); // 获取参数元数据
            Object[] args = point.getArgs(); // 获取参数值

            if (parameters == null || args == null) return null;

            for (int i = 0; i < parameters.length; i++) {
                if (i >= args.length) break;

                Parameter param = parameters[i];
                Object argValue = args[i];

                // 只关注 Long 类型的参数
                if (argValue instanceof Long) {
                    // 1. 优先取路径参数 (例如 /user/delete/{userId})
                    PathVariable pathVar = param.getAnnotation(PathVariable.class);
                    if (pathVar != null) {
                        return (Long) argValue;
                    }
                    // 2. 其次根据参数名判断
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
     * 核心逻辑：解析操作详情备注
     * 根据不同的方法名和参数类型，生成可读性强的日志备注
     */
    private String resolveDetailRemark(ProceedingJoinPoint point, String oldData, Object result) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            String methodName = signature.getName();

            // ========== 特殊处理：切换定时任务状态 ==========
            if ("toggleSchedule".equals(methodName)) {
                if (result != null) {
                    try {
                        // 利用反射获取返回值 R 中的 data 字段 (假设返回类型为 R<Boolean>)
                        // 这样做是为了解耦，避免在 AOP 类中强依赖具体的 R 类泛型结构
                        Method getDataMethod = result.getClass().getMethod("getData");
                        Object data = getDataMethod.invoke(result);
                        if (data instanceof Boolean) {
                            return ((Boolean) data) ? "动作: 启动定时任务" : "动作: 禁用定时任务";
                        }
                    } catch (Exception e) {
                        log.debug("解析定时任务状态返回值失败", e);
                    }
                }
                return "动作: 切换定时任务状态";
            }
            // ===========================================

            // ========== 特殊处理：重置密码 ==========
            if ("resetPassword".equals(methodName)) {
                return "动作: 重置为默认密码";
            }

            // ========== 通用处理：遍历参数生成描述 ==========
            Parameter[] parameters = signature.getMethod().getParameters();
            Object[] args = point.getArgs();

            if (args == null || parameters == null) return "";

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < parameters.length; i++) {
                if (i >= args.length) break;

                Object arg = args[i];
                Parameter param = parameters[i];

                // 获取参数名（支持 @RequestParam 重命名的情况）
                String paramName = param.getName();
                RequestParam requestParam = param.getAnnotation(RequestParam.class);
                if (requestParam != null) {
                    if (!requestParam.value().isEmpty()) {
                        paramName = requestParam.value();
                    } else if (!requestParam.name().isEmpty()) {
                        paramName = requestParam.name();
                    }
                }

                // 1. 处理封禁/解封请求
                if (arg instanceof UpdateStatusReq) {
                    UpdateStatusReq req = (UpdateStatusReq) arg;
                    if (req.getStatus() != null) {
                        return req.getStatus() == 1 ? "动作: 解封" : "动作: 封禁";
                    }
                }

                // 2. 处理修改昵称请求 (结合 oldData)
                if (arg instanceof UpdateNicknameReq) {
                    UpdateNicknameReq req = (UpdateNicknameReq) arg;
                    String oldStr = (oldData == null) ? "未知" : oldData;
                    return "原昵称: " + oldStr + " -> 新昵称: " + req.getNickname();
                }

                // 3. 处理其他通用参数 (如定时任务的 jobType, days)
                if ("jobType".equals(paramName) && arg instanceof String) {
                    sb.append("类型: ").append(arg);
                }
                if ("days".equals(paramName) && arg instanceof Integer) {
                    sb.append(", 天数: ").append(arg);
                }
                // 处理重跑任务 ID
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