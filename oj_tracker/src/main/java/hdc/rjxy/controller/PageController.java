package hdc.rjxy.controller;

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
}