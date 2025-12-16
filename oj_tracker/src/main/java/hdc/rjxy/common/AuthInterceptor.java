package hdc.rjxy.common;

import hdc.rjxy.pojo.UserSession;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;

import static hdc.rjxy.controller.AuthController.LOGIN_USER;

public class AuthInterceptor implements HandlerInterceptor {

    private boolean isPublicPath(String path) {
        return path.equals("/api/ping")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/register");
    }

    private boolean isAllowedWhenMustChangePwd(String path) {
        return path.equals("/api/me")
                || path.equals("/api/auth/change-password")
                || path.equals("/api/auth/logout");
    }

    private void writeJson(HttpServletResponse resp, int httpStatus, String json) throws Exception {
        resp.setStatus(httpStatus);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(json);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty()) {
            path = path.substring(ctx.length());
        }

        if (isPublicPath(path)) return true;

        HttpSession session = request.getSession(false);
        UserSession u = session == null ? null : (UserSession) session.getAttribute(LOGIN_USER);
        if (u == null) {
            writeJson(response, 401, "{\"code\":401,\"msg\":\"未登录\"}");
            return false;
        }

        // 禁用用户：登录后如果被禁用了，也直接拦住
        if (u.getStatus() != null && u.getStatus() == 0) {
            writeJson(response, 403, "{\"code\":403,\"msg\":\"账号已被禁用\"}");
            return false;
        }

        // 强制改密限制
        if (u.getMustChangePassword() != null && u.getMustChangePassword() == 1) {
            if (!isAllowedWhenMustChangePwd(path)) {
                writeJson(response, 403, "{\"code\":403,\"msg\":\"请先修改密码\"}");
                return false;
            }
        }

        // 管理员接口限制
        if (path.startsWith("/api/admin")) {
            if (!"ADMIN".equals(u.getRole())) {
                writeJson(response, 403, "{\"code\":403,\"msg\":\"无权限\"}");
                return false;
            }
        }

        return true;
    }
}
