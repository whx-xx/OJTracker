package hdc.rjxy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
    //todo 轻量同步后summary为空，heatmap填充了一堆空数据，history为空，每日/周题目和时间线有数据了。
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