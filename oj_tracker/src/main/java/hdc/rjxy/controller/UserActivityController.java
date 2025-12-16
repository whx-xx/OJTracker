package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.vo.HeatmapDayVO;
import hdc.rjxy.service.UserActivityService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/activity")
public class UserActivityController {

    private final UserActivityService userActivityService;

    public UserActivityController(UserActivityService userActivityService) {
        this.userActivityService = userActivityService;
    }
    public static final String LOGIN_USER = "LOGIN_USER";
    @GetMapping("/heatmap")
    public R<List<HeatmapDayVO>> heatmap(
            @RequestParam("platformCode") String platformCode,
            @RequestParam(value = "days", defaultValue = "90") int days,
            HttpSession session
    ) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");

        return R.ok(userActivityService.heatmap(me.getId(), platformCode, days));
    }

}
