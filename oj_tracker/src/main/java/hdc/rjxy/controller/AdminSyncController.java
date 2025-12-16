package hdc.rjxy.controller;

import hdc.rjxy.common.PageResult;
import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.vo.SyncJobDetailVO;
import hdc.rjxy.pojo.vo.SyncJobLogVO;
import hdc.rjxy.pojo.vo.SyncOverviewVO;
import hdc.rjxy.service.SyncService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static hdc.rjxy.controller.AuthController.LOGIN_USER;

@RestController
@RequestMapping("/api/admin/sync")
public class AdminSyncController {

    private final SyncService syncService;

    public AdminSyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping("/jobs")
    public R<PageResult<SyncJobLogVO>> jobs(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize,
            @RequestParam(value = "jobType", required = false) String jobType,
            HttpSession session
    ) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        if (!"ADMIN".equals(me.getRole())) return R.fail(403, "无权限");

        long total = syncService.countJobs(jobType);
        List<SyncJobLogVO> list = syncService.pageJobs(page, pageSize, jobType);

        PageResult<SyncJobLogVO> pr = new PageResult<>();
        pr.setTotal(total);
        pr.setList(list);
        return R.ok(pr);
    }


    @GetMapping("/jobs/{jobId}")
    public R<SyncJobDetailVO> jobDetail(@PathVariable(value = "jobId") Long jobId, HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        if (!"ADMIN".equals(me.getRole())) return R.fail(403, "无权限");

        return R.ok(syncService.jobDetail(jobId));
    }

    @PostMapping("/run")
    public R<Long> run(@RequestParam("jobType") String jobType,
                       @RequestParam(value = "days", required = false) Integer days,
                       HttpSession session) {

        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        if (!"ADMIN".equals(me.getRole())) return R.fail(403, "无权限");

        if ("RATING_SYNC".equalsIgnoreCase(jobType)) {
            return R.ok(syncService.runCfRatingSync("MANUAL"));
        }

        if ("DAILY_SYNC".equalsIgnoreCase(jobType)) {
            int d = (days == null) ? 3 : days;   // 默认 3 天
            return R.ok(syncService.runCfDailySync("MANUAL", d));
        }

        return R.fail(400, "未知 jobType: " + jobType);
    }

    @GetMapping("/overview")
    public R<SyncOverviewVO> overview(
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpSession session
    ) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        if (!"ADMIN".equals(me.getRole())) return R.fail(403, "无权限");

        return R.ok(syncService.overview(limit));
    }

    @PostMapping("/rerun")
    public R<Long> rerun(@RequestParam("jobId") Long jobId, HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        if (!"ADMIN".equals(me.getRole())) return R.fail(403, "无权限");

        return R.ok(syncService.rerunFailedUsers(jobId, "MANUAL_RERUN"));
    }


}
