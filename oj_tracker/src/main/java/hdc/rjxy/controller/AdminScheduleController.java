package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.task.SyncScheduler;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import static hdc.rjxy.controller.UserRatingController.LOGIN_USER;

@RestController
@RequestMapping("/api/admin/schedule")
public class AdminScheduleController {

    private final SyncScheduler scheduler;

    public AdminScheduleController(SyncScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostMapping("/enable")
    public R<Void> enable(@RequestParam("enabled") boolean enabled, HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        if (!"ADMIN".equals(me.getRole())) return R.fail(403, "无权限");

        scheduler.setEnabled(enabled);
        return R.ok(null);
    }
}
