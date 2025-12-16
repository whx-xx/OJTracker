package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.vo.RatingHistoryPointVO;
import hdc.rjxy.service.UserRatingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/rating")
public class UserRatingController {

    private final UserRatingService userRatingService;
    public static final String LOGIN_USER = "LOGIN_USER";

    public UserRatingController(UserRatingService userRatingService) {
        this.userRatingService = userRatingService;
    }

    @GetMapping("/history")
    public R<List<RatingHistoryPointVO>> history(
            @RequestParam("platformCode") String platformCode,
            @RequestParam(value = "days", defaultValue = "365") int days,
            HttpSession session
    ) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");

        return R.ok(userRatingService.history(me.getId(), platformCode, days));
    }
}
