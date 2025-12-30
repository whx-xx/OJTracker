package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.vo.RatingPointVO;
import hdc.rjxy.pojo.vo.UserStatsSummaryVO;
import hdc.rjxy.service.UserStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户统计", description = "个人主页汇总卡片数据")
@RestController
@RequestMapping("/api/users")
public class UserStatsController {

    private final UserStatsService userStatsService;

    public UserStatsController(UserStatsService userStatsService) {
        this.userStatsService = userStatsService;
    }

    @Operation(summary = "Rating 历史 (公开)", description = "用于展示指定用户的 Rating 趋势（如在团队榜单中点击）")
    @GetMapping("/{userId}/rating-history")
    public R<List<RatingPointVO>> ratingHistory(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "platformCode", required = false, defaultValue = "CF") String platformCode,
            @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit
    ) {
        return R.ok(userStatsService.ratingHistory(userId, platformCode, limit));
    }

    @Operation(summary = "个人数据汇总", description = "获取当前用户的刷题统计概览")
    @GetMapping("/summary")
    public R<UserStatsSummaryVO> summary(@RequestParam(value = "platformCode", required = false) String platformCode,
                                         @RequestParam(value = "days", defaultValue = "5000") int days,
                                         HttpSession session) {
        UserSession me = (UserSession) session.getAttribute("user");
        if (me == null) return R.fail(401, "未登录");

        return R.ok(userStatsService.summary(me.getId(), platformCode, days));
    }
}