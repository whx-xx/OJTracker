package hdc.rjxy.common;

import hdc.rjxy.pojo.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

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

        if (user != null) {
            return true; // 已登录，放行
        }

        // 未登录的处理
        // 如果是 AJAX 请求 (API)，返回 401 状态码
        String uri = request.getRequestURI();
        if (uri.startsWith(request.getContextPath() + "/api/")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\",\"data\":null}");
            return false;
        }

        // 如果是页面请求，重定向到登录页
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}