package hdc.rjxy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;

@Controller // 这里不是 @RestController
public class PageController {

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

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }
}