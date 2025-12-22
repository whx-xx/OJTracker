package hdc.rjxy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 负责页面跳转的控制器
 */
@Controller
public class PageController {

    /**
     * 登录页and注册页
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    

}