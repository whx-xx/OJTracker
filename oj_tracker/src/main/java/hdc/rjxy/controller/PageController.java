package hdc.rjxy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面路由控制器
 * 职责：只负责返回 Thymeleaf 模板名称，不处理 JSON 数据业务
 */
@Controller
public class PageController {

    // === 登录与公共页 ===
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String index() {
        return "layout";
    }

    // === 用户功能页 ===
    /**
     * 仪表盘 (Iframe 内容)
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/views/profile")
    public String profile() {
        return "user/profile";
    }

    @GetMapping("/views/rankings")
    public String rankings() {
        return "user/rankings";
    }

    /**
     * [新增] 做题记录详情页
     */
    @GetMapping("/views/submissions")
    public String submissions() {
        return "user/submissions";
    }

    // === 管理员功能页 ===
    @GetMapping("/views/users")
    public String userList() {
        return "admin/user-list";
    }

    @GetMapping("/views/sync")
    public String syncManage() {
        return "admin/sync-manage";
    }

    @GetMapping("/views/logs")
    public String opLogs() {
        return "admin/op-logs";
    }
}