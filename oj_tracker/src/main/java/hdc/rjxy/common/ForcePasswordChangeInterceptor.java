package hdc.rjxy.common;

import hdc.rjxy.pojo.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * 强制修改密码拦截器
 */
public class ForcePasswordChangeInterceptor implements HandlerInterceptor {

    // 白名单：即使需要改密码，也允许访问的路径
    private static final List<String> WHITELIST = Arrays.asList(
            "/",
            "/login",                   // 登录页
            "/logout",                  // 注销
            "/api/auth/logout",         // 注销接口
            "/api/auth/change-password",// 修改密码接口
            "/api/me",
            "/api/auth/current",
            "/views/profile",           // 个人中心页面
            "/css/**", "/js/**", "/images/**", "/lib/**", "/favicon.ico" // 静态资源
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 1. 如果在白名单中，直接放行
        // 注意：简单的字符串包含判断，实际项目中可以使用 AntPathMatcher
        for (String white : WHITELIST) {
            if (uri.startsWith(white) || uri.equals("/")) {
                return true;
            }
        }

        // 2. 获取当前用户
        HttpSession session = request.getSession(false);
        if (session != null) {
            UserSession user = (UserSession) session.getAttribute("user");

            // 3. 核心判断：如果必须修改密码 (mustChangePassword == 1)
            if (user != null && user.getMustChangePassword() != null && user.getMustChangePassword() == 1) {

                // 如果是 AJAX 请求 (Axios)，返回 403 和特定 JSON
                if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\": 403, \"msg\": \"为了您的账户安全，请先修改初始密码\", \"data\": \"MUST_CHANGE_PASSWORD\"}");
                    return false;
                }

                // 如果是普通页面请求，重定向到个人中心 (如果是 Iframe 架构，通常前端 Layout 会处理，这里做个保底)
                // 这里我们通常不重定向，因为是 SPA/Iframe 架构，直接拦截即可
                return false;
            }
        }

        return true;
    }
}