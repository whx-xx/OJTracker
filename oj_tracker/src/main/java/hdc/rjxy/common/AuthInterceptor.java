package hdc.rjxy.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import hdc.rjxy.pojo.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.nio.charset.StandardCharsets;

import static hdc.rjxy.controller.AuthController.LOGIN_USER;

@Component // 声明为 Spring 组件
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    // 构造函数注入 ObjectMapper
    public AuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private boolean isPublicPath(String path) {
        return path.equals("/api/ping")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/register");
    }

    private boolean isAllowedWhenMustChangePwd(String path) {
        return path.equals("/api/me")
                || path.equals("/api/auth/change-password")
                || path.equals("/api/auth/logout")
                || path.equals("/settings"); // 允许进入设置页改密
    }

    private void writeJson(HttpServletResponse resp, int httpStatus, Object data) throws Exception {
        resp.setStatus(httpStatus);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json;charset=UTF-8");
        // 使用注入的 objectMapper 转 JSON
        resp.getWriter().write(objectMapper.writeValueAsString(data));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }

        if (isPublicPath(path)) return true;

        HttpSession session = request.getSession(false);
        UserSession u = session == null ? null : (UserSession) session.getAttribute(LOGIN_USER);

        // 1. 处理未登录
        if (u == null) {
            if (path.startsWith("/api/")) {
                writeJson(response, 401, R.fail(401, "未登录"));
            } else {
                response.sendRedirect(request.getContextPath() + "/login"); // 页面请求重定向
            }
            return false;
        }

        // 2. 账号被禁用处理
        if (u.getStatus() != null && u.getStatus() == 0) {
            if (path.startsWith("/api/")) {
                writeJson(response, 403, R.fail(403, "账号已被禁用"));
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return false;
        }

        // 3. 强制改密限制
        if (u.getMustChangePassword() != null && u.getMustChangePassword() == 1) {
            if (!isAllowedWhenMustChangePwd(path)) {
                if (path.startsWith("/api/")) {
                    writeJson(response, 403, R.fail(403, "请先修改密码"));
                } else {
                    response.sendRedirect(request.getContextPath() + "/settings"); // 强制跳转设置页
                }
                return false;
            }
        }

        // 4. 管理员权限限制
        if (path.startsWith("/api/admin")) {
            if (!"ADMIN".equals(u.getRole())) {
                writeJson(response, 403, R.fail(403, "无权限"));
                return false;
            }
        }

        return true;
    }
}