package hdc.rjxy.common;

import hdc.rjxy.pojo.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

public class ForcePasswordChangeInterceptor implements HandlerInterceptor {

    // 1. 从白名单中移除单独的 "/"，避免 startsWith 误伤
    private static final List<String> WHITELIST_PREFIXES = Arrays.asList(
            "/login",
            "/logout",
            "/api/auth/logout",
            "/api/auth/change-password",
            "/api/me",
            "/api/auth/current",
            "/views/profile", // 只允许个人中心
            "/css/", "/js/", "/images/", "/lib/", "/favicon.ico" // 静态资源建议加 / 结尾更精确
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 根路径完全匹配（只允许首页，不允许 /views/rankings）
        if (uri.equals("/")) {
            return true;
        }

        // 前缀匹配（遍历列表）
        for (String white : WHITELIST_PREFIXES) {
            if (uri.startsWith(white)) {
                return true;
            }
        }

        // --- 以下逻辑保持不变 ---

        // 3. 获取当前用户
        HttpSession session = request.getSession(false);
        if (session != null) {
            UserSession user = (UserSession) session.getAttribute("user");

            // 4. 核心判断：如果必须修改密码
            if (user != null && user.getMustChangePassword() != null && user.getMustChangePassword() == 1) {

                // AJAX 请求返回 JSON
                if (isAjax(request)) {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\": 403, \"msg\": \"为了您的账户安全，请先修改初始密码\", \"data\": \"MUST_CHANGE_PASSWORD\"}");
                    return false;
                }

                // 普通页面请求：这里很关键
                // 如果用户试图访问 rankings，拦截并重定向回 profile 页面（或者是修改密码页）
                // 防止无限重定向：我们在上面的白名单里已经允许了 /views/profile
                response.sendRedirect(request.getContextPath() + "/views/profile");
                return false;
            }
        }

        return true;
    }

    private boolean isAjax(HttpServletRequest request) {
        String requestType = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equals(requestType) ||
                (accept != null && accept.contains("application/json"));
    }
}