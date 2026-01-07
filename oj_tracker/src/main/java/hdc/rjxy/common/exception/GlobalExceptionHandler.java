package hdc.rjxy.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务逻辑异常 (如参数校验失败、账号禁用等)
     * 解决前端 Axios 遇到 500 错误直接进 catch 的问题
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK) // <--- 返回 HTTP 200，确保前端进入 .then()
    public Object handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        // 如果是 API 请求，返回 JSON
        if (isAjax(request)) {
            // 这里返回 code 400 或其他你约定的失败码，msg 直接使用异常信息
            Map<String, Object> map = new HashMap<>();
            map.put("code", 400);
            map.put("msg", e.getMessage()); // 直接显示 "账号已被禁用"，不加前缀
            map.put("success", false);
            return ResponseEntity.ok(map);
        }

        // 如果是页面请求，可以跳到 500 页面或者登录页并带上错误信息
        return "error/500";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Object handle404(NoHandlerFoundException e, HttpServletRequest request) {
        if (isAjax(request)) {
            return buildJsonResult(404, "请求的资源不存在: " + request.getRequestURI());
        }
        return "error/404";
    }

    // --- 处理 403 权限不足 ---
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Object handle403(AccessDeniedException e, HttpServletRequest request) {
        // 如果是 API 请求，返回 JSON
        if (isAjax(request)) {
            return buildJsonResult(403, "权限不足: " + e.getMessage());
        }
        // 如果是页面请求，返回 403 页面
        return "error/403";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object handleGlobalException(Exception e, HttpServletRequest request, Model model) {
        log.error("[GlobalEx] 捕获未处理异常: {} {}", request.getMethod(), request.getRequestURI(), e);
        if (isAjax(request)) {
            return buildJsonResult(500, "服务器内部错误: " + e.getMessage());
        }
        model.addAttribute("exception", e.getMessage());
        model.addAttribute("url", request.getRequestURI());
        return "error/500";
    }

    private boolean isAjax(HttpServletRequest request) {
        String requestType = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        if ("XMLHttpRequest".equals(requestType)) return true;
        if (accept != null && accept.contains("application/json")) return true;
        if (uri.startsWith("/api/") || uri.startsWith("/json/")) return true;
        return false;
    }

    private ResponseEntity<Map<String, Object>> buildJsonResult(int status, String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", status);
        map.put("msg", message);
        map.put("success", false);
        return ResponseEntity.status(status).body(map);
    }
}