package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.vo.RatingPointVO;
import hdc.rjxy.pojo.vo.UserStatsSummaryVO;
import hdc.rjxy.service.UserStatsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserStatsController {

    private final UserStatsService userStatsService;
    public static final String LOGIN_USER = "LOGIN_USER";
    public UserStatsController(UserStatsService userStatsService) {
        this.userStatsService = userStatsService;
    }

    @GetMapping("/{userId}/rating-history")
    public R<List<RatingPointVO>> ratingHistory(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "platformCode", required = false, defaultValue = "CF") String platformCode,
            @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit
    ) {
        return R.ok(userStatsService.ratingHistory(userId, platformCode, limit));
    }

    @GetMapping("/summary")
    public R<UserStatsSummaryVO> summary(@RequestParam(value = "platformCode", required = false) String platformCode,
                                         @RequestParam(value = "days", defaultValue = "5000") int days,
                                         HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");

        return R.ok(userStatsService.summary(me.getId(), platformCode, days));
    }
}
