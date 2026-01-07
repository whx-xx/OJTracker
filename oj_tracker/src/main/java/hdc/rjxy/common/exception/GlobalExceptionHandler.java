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

    // ... (保留之前的 handle404 代码) ...
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Object handle404(NoHandlerFoundException e, HttpServletRequest request) {
        if (isAjax(request)) {
            return buildJsonResult(404, "请求的资源不存在: " + request.getRequestURI());
        }
        return "error/404";
    }

    // --- 新增：处理 403 权限不足 ---
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

    // ... (保留之前的 handleGlobalException 代码) ...
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