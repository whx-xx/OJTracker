package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.vo.WeeklyProblemVO;
import hdc.rjxy.service.UserProblemService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/problems")
public class UserProblemController {

    public static final String LOGIN_USER = "LOGIN_USER";

    private final UserProblemService userProblemService;

    public UserProblemController(UserProblemService userProblemService) {
        this.userProblemService = userProblemService;
    }

    @GetMapping("/week")
    public R<List<WeeklyProblemVO>> week(@RequestParam(value = "platformCode", defaultValue = "CF") String platformCode,
                                         HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");

        return R.ok(userProblemService.week(me.getId(), platformCode));
    }
}
