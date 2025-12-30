package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.vo.HeatmapDayVO;
import hdc.rjxy.service.UserActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户活跃度", description = "热力图数据")
@RestController
@RequestMapping("/api/user/activity")
public class UserActivityController {

    private final UserActivityService userActivityService;

    public UserActivityController(UserActivityService userActivityService) {
        this.userActivityService = userActivityService;
    }

    @Operation(summary = "获取热力图数据")
    @GetMapping("/heatmap")
    public R<List<HeatmapDayVO>> heatmap(
            @RequestParam("platformCode") String platformCode,
            @RequestParam(value = "days", defaultValue = "90") int days,
            HttpSession session
    ) {
        UserSession me = (UserSession) session.getAttribute("user");
        if (me == null) return R.fail(401, "未登录");

        return R.ok(userActivityService.heatmap(me.getId(), platformCode, days));
    }
}