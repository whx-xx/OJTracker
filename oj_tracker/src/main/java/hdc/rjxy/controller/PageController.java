package hdc.rjxy.controller;

import hdc.rjxy.pojo.UserSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;

@Controller // 这里不是 @RestController
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // 寻找 /WEB-INF/templates/login.html
    }

    @GetMapping("/dashboard")
    public String dashboardPage(HttpSession session) {
        // 检查 Session 中是否有登录信息
        if (session.getAttribute("LOGIN_USER") == null) {
            return "redirect:/login";
        }
        return "dashboard"; // 对应 templates/dashboard.html
    }

    @GetMapping("/teams")
    public String teamsPage(HttpSession session) {
        // 检查登录状态
        if (session.getAttribute("LOGIN_USER") == null) {
            return "redirect:/login";
        }
        return "teams"; // 对应 WEB-INF/templates/teams.html
    }

    @GetMapping("/settings")
    public String settingsPage(HttpSession session) {
        if (session.getAttribute("LOGIN_USER") == null) {
            return "redirect:/login";
        }
        return "settings"; // 对应 templates/settings.html
    }

    @GetMapping("/admin/users")
    public String adminUsersPage(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("LOGIN_USER");
        // 权限校验：未登录或不是管理员，重定向到看板
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/dashboard";
        }
        return "admin-users"; // 对应 WEB-INF/templates/admin-users.html
    }

    @GetMapping("/admin/sync")
    public String adminSyncPage(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute(AuthController.LOGIN_USER);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/dashboard";
        }
        return "admin-sync"; // 返回 WEB-INF/templates/admin-sync.html
    }

    @GetMapping("/admin/op-logs")
    public String adminOpLogsPage(HttpSession session) {
        // 简单鉴权，实际上 AuthInterceptor 会再次拦截
        UserSession user = (UserSession) session.getAttribute(AuthController.LOGIN_USER);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/dashboard";
        }
        return "admin-op-logs"; // 对应 templates/admin-op-logs.html
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register"; // 对应 WEB-INF/templates/register.html
    }
}