package hdc.rjxy.controller;

import hdc.rjxy.cf.CfClientException;
import hdc.rjxy.common.R;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务参数错误：平台不存在 / 未绑定账号 / days不合法 等
     * 统一当作 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<?> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(400, safeMsg(e.getMessage(), "参数错误"));
    }

    /**
     * Codeforces 客户端错误（你已经在 SyncService 里 catch 并写 log 了）
     * 这里主要给“前端查询类接口”兜底，比如 /week /heatmap /summary /history
     *
     * 约定：
     * - CF_HTTP_502/503/504 -> 502
     * - CF_TIMEOUT/CF_CONNECTION_RESET -> 504
     * - CF_PARSE_ERROR -> 502
     * - HANDLE_MISSING / NOT_BOUND 等 -> 400
     * - 其它 CF_* -> 502
     */
    @ExceptionHandler(CfClientException.class)
    public R<?> handleCfClient(CfClientException e) {
        String code = e.getCode();
        int http = mapCfCodeToHttp(code);
        // msg 保留原始信息（你 error_message 是 text，OK）
        return R.fail(http, safeMsg(e.getMessage(), "Codeforces 请求失败"));
    }

    /**
     * 兜底：避免返回 Tomcat HTML 500
     * 这里不把异常细节直接给前端（安全&体验），但你可以在控制台打印
     */
    @ExceptionHandler(Exception.class)
    public R<?> handleOther(Exception e, HttpServletRequest req) {
        // 你不想引日志框架也行，先打印
        System.err.println("[500] " + req.getMethod() + " " + req.getRequestURI());
        e.printStackTrace();
        return R.fail(500, "服务器内部错误");
    }

    // ----------------- helpers -----------------

    private int mapCfCodeToHttp(String code) {
        if (code == null) return 502;

        // 你现在的细分建议：CF_HTTP_502、CF_TIMEOUT、CF_PARSE_ERROR、HANDLE_MISSING...
        if (code.startsWith("CF_HTTP_")) {
            // 提取 HTTP 状态码
            try {
                int s = Integer.parseInt(code.substring("CF_HTTP_".length()));
                // 典型：502/503/504
                if (s >= 400 && s <= 599) return (s == 500 ? 502 : s);
            } catch (Exception ignored) {}
            return 502;
        }

        // 超时 / 连接重置
        if (Objects.equals(code, "CF_TIMEOUT") || Objects.equals(code, "CF_CONNECTION_RESET")) {
            return 504;
        }

        // JSON 解析失败/字段不匹配
        if (Objects.equals(code, "CF_PARSE_ERROR")) {
            return 502;
        }

        // 用户自身原因 -> 400
        if (Objects.equals(code, "HANDLE_MISSING") || Objects.equals(code, "NOT_BOUND") || Objects.equals(code, "HANDLE_INVALID")) {
            return 400;
        }

        // 默认认为是上游失败
        if (code.startsWith("CF_")) return 502;

        // 非 CF code
        return 500;
    }

    private String safeMsg(String msg, String fallback) {
        if (msg == null) return fallback;
        String s = msg.trim();
        return s.isEmpty() ? fallback : s;
    }
}
