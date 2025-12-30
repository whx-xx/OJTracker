package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.vo.RatingHistoryPointVO;
import hdc.rjxy.service.UserRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Rating 历史", description = "用户积分曲线")
@RestController
@RequestMapping("/api/user/rating")
public class UserRatingController {

    private final UserRatingService userRatingService;

    public UserRatingController(UserRatingService userRatingService) {
        this.userRatingService = userRatingService;
    }

    @Operation(summary = "获取积分历史")
    @GetMapping("/history")
    public R<List<RatingHistoryPointVO>> history(
            @RequestParam(value = "platformCode", required = false) String platformCode,
            @RequestParam(value = "days", defaultValue = "365") int days,
            HttpSession session
    ) {
        UserSession me = (UserSession) session.getAttribute("user");
        if (me == null) return R.fail(401, "未登录");

        return R.ok(userRatingService.history(me.getId(), platformCode, days));
    }
}