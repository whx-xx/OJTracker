package hdc.rjxy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    //todo 轻量同步后数据没有获取到好像是，或者是获取到了没有显示到页面。

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

    // --- 以下是后续模块的占位符，你可以先创建简单的html文件或暂不创建 ---

    @GetMapping("/views/users")
    public String userList() { return "admin/user-list"; } // 需要后续创建

    @GetMapping("/views/sync")
    public String syncManage() { return "admin/sync-manage"; } // 需要后续创建

    @GetMapping("/views/profile")
    public String profile() { return "user/profile"; } // 需要后续创建

    @GetMapping("/views/rankings")
    public String rankings() { return "user/rankings"; } // 需要后续创建
    //todo 个人信息传头像，在header和排行里显示
}