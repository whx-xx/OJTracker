package hdc.rjxy.aop;

import hdc.rjxy.common.R;
import hdc.rjxy.mapper.AdminOpLogMapper;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.dto.UpdateNicknameReq; // 确保引入了你的DTO
import hdc.rjxy.pojo.dto.UpdateStatusReq;   // 确保引入了你的DTO
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static hdc.rjxy.controller.AuthController.LOGIN_USER;

@Aspect
@Component
public class AdminLogAspect {

    private final AdminOpLogMapper adminOpLogMapper;

    public AdminLogAspect(AdminOpLogMapper adminOpLogMapper) {
        this.adminOpLogMapper = adminOpLogMapper;
    }

    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "result")
    public void recordLog(JoinPoint joinPoint, LogAdminOp controllerLog, Object result) {
        // 1. 获取 Web 上下文
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return;
        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);
        if (session == null) return;

        // 2. 获取当前操作的管理员
        UserSession admin = (UserSession) session.getAttribute(LOGIN_USER);
        if (admin == null) return;

        // 3. 解析参数 (目标用户ID 和 DTO)
        Object[] args = joinPoint.getArgs();
        Long targetUserId = null;
        Object reqDto = null; // 用于存储提取到的请求对象 (StatusReq 或 NicknameReq)

        for (Object arg : args) {
            // 提取第一个 Long 类型作为 targetUserId
            if (targetUserId == null && arg instanceof Long) {
                targetUserId = (Long) arg;
            }
            // 提取 DTO 对象 (用于后续生成 remark)
            if (arg instanceof UpdateStatusReq || arg instanceof UpdateNicknameReq) {
                reqDto = arg;
            }
        }

        // 4. 获取基础信息
        String ip = request.getRemoteAddr();
        String opType = controllerLog.opType();

        // 5. 生成 Remark (核心逻辑优化)
        String remark = buildRemark(opType, result, reqDto);

        // 6. 写入数据库
        adminOpLogMapper.insert(admin.getId(), targetUserId, opType, ip, remark);
    }

    /**
     * 根据操作类型、执行结果和请求参数，构建详细的备注信息
     */
    private String buildRemark(String opType, Object result, Object reqDto) {
        // 1. 先判断业务是否执行成功
        if (result instanceof R) {
            R<?> r = (R<?>) result;
            if (r.getCode() != 200) {
                return "FAIL: " + r.getMsg(); // 如果失败，直接记录错误信息
            }
        }

        // 2. 成功后，根据 opType 定制文案
        switch (opType) {
            case "RESET_PWD":
                return "重置密码为默认(000000)";

            case "UPDATE_STATUS":
                if (reqDto instanceof UpdateStatusReq) {
                    Integer status = ((UpdateStatusReq) reqDto).getStatus();
                    // 假设 1=正常/启用, 0=禁用
                    return (status != null && status == 1) ? "启用用户" : "禁用用户";
                }
                break;

            case "UPDATE_NICKNAME":
                if (reqDto instanceof UpdateNicknameReq) {
                    String nickname = ((UpdateNicknameReq) reqDto).getNickname();
                    return "修改昵称为: " + nickname;
                }
                break;

            // 你可以继续扩展其他类型...
            default:
                return "SUCCESS";
        }

        return "SUCCESS"; // 兜底
    }
}