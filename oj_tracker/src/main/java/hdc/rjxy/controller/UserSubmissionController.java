package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.vo.RefreshResultVO;
import hdc.rjxy.pojo.vo.SubmissionTimelineVO;
import hdc.rjxy.service.UserSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/submissions")
public class UserSubmissionController {

    public static final String LOGIN_USER = "LOGIN_USER";
    private final UserSubmissionService userSubmissionService;

    public UserSubmissionController(UserSubmissionService userSubmissionService) {
        this.userSubmissionService = userSubmissionService;
    }

    /** 时间线：TODAY / WEEK（默认 WEEK） */
    @GetMapping("/timeline")
    public R<List<SubmissionTimelineVO>> timeline(
            @RequestParam(value = "platformCode", defaultValue = "CF") String platformCode,
            @RequestParam(value = "range", defaultValue = "WEEK") String range,
            @RequestParam(value = "limit", defaultValue = "50") Integer limit,
            HttpSession session
    ) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");

        return R.ok(userSubmissionService.timeline(me.getId(), platformCode, range, limit));
    }

    /** 打开页面轻量补一次：拉最新 N 条增量写库 */
    @PostMapping("/refresh")
    public R<RefreshResultVO> refresh(
            @RequestParam(value = "platformCode", defaultValue = "CF") String platformCode,
            @RequestParam(value = "count", defaultValue = "200") Integer count,
            HttpSession session
    ) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");

        return R.ok(userSubmissionService.refreshLight(me.getId(), platformCode, count));
    }
}
