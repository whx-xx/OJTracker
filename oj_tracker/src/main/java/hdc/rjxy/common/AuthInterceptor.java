package hdc.rjxy.common;

import hdc.rjxy.common.exception.AccessDeniedException;
import hdc.rjxy.pojo.UserSession; // 注意这里导入的是 UserSession
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果是 OPTIONS 请求（跨域预检），直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        UserSession user = (session != null) ? (UserSession) session.getAttribute("user") : null;

        // 1. 检查是否登录
        if (user == null) {
            // 如果是 AJAX 请求 (API)，返回 401
            if (isAjax(request)) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\",\"data\":null}");
                return false;
            }
            // 否则重定向到登录页
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        // 2. 检查管理员权限
        String uri = request.getRequestURI();
        if (uri.startsWith("/admin/") || uri.startsWith(request.getContextPath() + "/admin/")) {
            if (!Objects.equals(user.getRole(), "ADMIN")) {
                throw new AccessDeniedException("您不是管理员，禁止访问后台！");
            }
        }

        return true; // 放行
    }

    private boolean isAjax(HttpServletRequest request) {
        String requestType = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();

        return "XMLHttpRequest".equals(requestType) ||
                (accept != null && accept.contains("application/json")) ||
                uri.startsWith(request.getContextPath() + "/api/");
    }
}