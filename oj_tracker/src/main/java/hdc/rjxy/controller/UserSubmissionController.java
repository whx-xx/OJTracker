package hdc.rjxy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /**
     * 获取提交时间线数据 (分页版 + 搜索)
     */
    @Operation(summary = "获取提交时间线", description = "range=TODAY/WEEK/MONTH/ALL, keyword=搜索词")
    @GetMapping("/timeline")
    public R<Page<SubmissionTimelineVO>> timeline(
            @RequestParam(required = false, defaultValue = "CF") String platform,
            @RequestParam(required = false, defaultValue = "TODAY") String range,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            HttpSession session
    ) {
        UserSession me = (UserSession) session.getAttribute("user");
        if (me == null) return R.fail(401, "未登录");
        return R.ok(userSubmissionService.timeline(me.getId(), platform, range, keyword, page, size));
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