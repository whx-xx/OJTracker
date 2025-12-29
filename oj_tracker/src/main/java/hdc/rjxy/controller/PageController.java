package hdc.rjxy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    //todo 调度任务的返回结果不正确
    //todo 操作日志扩展
    //todo 添加jwt认证

    @GetMapping("/login")
    public String login() {
        return "login";
    }
    /**
     * 这里的 /dashboard 是纯粹的内容页，用于被 iframe 加载
     */
    @GetMapping({"/dashboard"})
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/")
    public String index() {
        return "layout";
    }

    @GetMapping("/views/users")
    public String userList() { return "admin/user-list"; }

    @GetMapping("/views/sync")
    public String syncManage() { return "admin/sync-manage"; }

    @GetMapping("/views/logs")
    public String opLogs() { return "admin/op-logs"; }

    @GetMapping("/views/profile")
    public String profile() { return "user/profile"; }

    @GetMapping("/views/rankings")
    public String rankings() { return "user/rankings"; }
}