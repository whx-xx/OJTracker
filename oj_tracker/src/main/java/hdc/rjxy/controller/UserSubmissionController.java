package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.vo.RefreshResultVO;
import hdc.rjxy.pojo.vo.SubmissionTimelineVO;
import hdc.rjxy.service.UserSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "提交记录", description = "用户做题记录与同步")
@RestController
@RequestMapping("/api/user/submissions")
public class UserSubmissionController {

    private final UserSubmissionService userSubmissionService;

    public UserSubmissionController(UserSubmissionService userSubmissionService) {
        this.userSubmissionService = userSubmissionService;
    }

    @Operation(summary = "获取提交时间线", description = "range=TODAY/WEEK")
    @GetMapping("/timeline")
    public R<List<SubmissionTimelineVO>> timeline(
            @RequestParam(value = "platformCode", defaultValue = "CF") String platformCode,
            @RequestParam(value = "range", defaultValue = "WEEK") String range,
            @RequestParam(value = "limit", defaultValue = "50") Integer limit,
            HttpSession session
    ) {
        UserSession me = (UserSession) session.getAttribute("user");
        if (me == null) return R.fail(401, "未登录");

        return R.ok(userSubmissionService.timeline(me.getId(), platformCode, range, limit));
    }

    @Operation(summary = "手动刷新数据", description = "触发增量同步（耗时操作）")
    @PostMapping("/refresh")
    public R<RefreshResultVO> refresh(
            @RequestParam(value = "platformCode", defaultValue = "CF") String platformCode,
            @RequestParam(value = "count", defaultValue = "200") Integer count,
            HttpSession session
    ) {
        UserSession me = (UserSession) session.getAttribute("user");
        if (me == null) return R.fail(401, "未登录");

        return R.ok(userSubmissionService.refreshLight(me.getId(), platformCode, count));
    }
}