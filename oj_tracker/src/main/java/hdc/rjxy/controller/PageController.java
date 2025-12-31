package hdc.rjxy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // 对应 login.html
    }

    @GetMapping("/me")
    public String mePage() {
        return "me";    // 对应 me.html
    }

    // 如果有主页
    @GetMapping("/")
    public String indexPage() {
        return "redirect:/me"; // 暂时跳到个人中心
    }
}