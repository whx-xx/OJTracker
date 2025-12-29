package hdc.rjxy.controller;

import hdc.rjxy.cf.CfClientException;
import hdc.rjxy.common.R;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * 全局异常处理器
 * 作用：拦截所有 Controller 抛出的异常，转换为标准 JSON 格式 (R)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 业务参数错误处理
     * 场景：Assert.notNull 失败 / 抛出 IllegalArgumentException
     * 返回：HTTP 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<?> handleIllegalArgument(IllegalArgumentException e) {
        // e.getMessage() 可能是 "id cannot be null"，我们包装一下
        return R.fail(400, safeMsg(e.getMessage(), "请求参数错误"));
    }

    /**
     * 2. Codeforces 客户端错误处理
     * 场景：调用 CF API 失败（超时、503、用户不存在等）
     * 策略：根据错误码 (code) 映射到合适的 HTTP 状态码
     */
    @ExceptionHandler(CfClientException.class)
    public R<?> handleCfClient(CfClientException e) {
        String code = e.getCode();
        int httpStatus = mapCfCodeToHttp(code);

        // 保留原始的错误信息 (e.getMessage())，方便前端展示具体原因
        return R.fail(httpStatus, safeMsg(e.getMessage(), "Codeforces 服务请求失败"));
    }

    /**
     * 3. 兜底异常处理
     * 场景：空指针 (NPE)、数据库错误、数组越界等未预料到的错误
     * 返回：HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public R<?> handleOther(Exception e, HttpServletRequest req) {
        // 在控制台打印堆栈，方便开发调试 (生产环境建议换成 log.error)
        System.err.println("[GlobalEx] 捕获未处理异常: " + req.getMethod() + " " + req.getRequestURI());
        e.printStackTrace();

        // 为了安全，不向前端暴露具体的 Java 堆栈信息
        return R.fail(500, "服务器内部错误，请联系管理员");
    }

    // ----------------- 辅助方法 -----------------

    /**
     * 将 CF 业务错误码映射为 HTTP 状态码
     */
    private int mapCfCodeToHttp(String code) {
        if (code == null) return 502;

        // 1. HTTP 错误透传 (如 CF_HTTP_503 -> 503)
        if (code.startsWith("CF_HTTP_")) {
            try {
                int s = Integer.parseInt(code.substring("CF_HTTP_".length()));
                if (s >= 400 && s <= 599) return (s == 500 ? 502 : s);
            } catch (Exception ignored) {}
            return 502;
        }

        // 2. 网络连接问题 -> 504 Gateway Timeout
        if (Objects.equals(code, "CF_TIMEOUT") || Objects.equals(code, "CF_CONNECTION_RESET")) {
            return 504;
        }

        // 3. 数据解析失败 -> 502 Bad Gateway
        if (Objects.equals(code, "CF_PARSE_ERROR")) {
            return 502;
        }

        // 4. 用户输入导致的问题 (如账号不存在) -> 400 Bad Request
        if (Objects.equals(code, "HANDLE_MISSING") ||
                Objects.equals(code, "NOT_BOUND") ||
                Objects.equals(code, "HANDLE_INVALID")) {
            return 400;
        }

        // 5. 其他 CF 错误默认 502
        if (code.startsWith("CF_")) return 502;

        return 500;
    }

    /**
     * 确保消息不为空
     */
    private String safeMsg(String msg, String fallback) {
        if (msg == null) return fallback;
        String s = msg.trim();
        return s.isEmpty() ? fallback : s;
    }
}