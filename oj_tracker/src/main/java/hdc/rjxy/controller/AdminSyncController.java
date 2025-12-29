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

    // 1. 任务列表分页
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

    // 2. [关键修复] 获取任务详情 (用于弹窗)
    // 修正 URL 映射以匹配前端: /api/admin/sync/jobs/{jobId}
    @GetMapping("/jobs/{jobId}")
    public R<SyncJobDetailVO> jobDetail(@PathVariable(value = "jobId") Long jobId, HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        if (!"ADMIN".equals(me.getRole())) return R.fail(403, "无权限");

        return R.ok(syncService.jobDetail(jobId));
    }

    // 3. 手动触发同步
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
            int d = (days == null) ? 3 : days;
            return R.ok(syncService.runCfDailySync("MANUAL", d));
        }

        return R.fail(400, "未知 jobType: " + jobType);
    }

    // 4. 概览数据
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

    // 5. 重跑失败任务
    @PostMapping("/rerun")
    public R<Long> rerun(@RequestParam("jobId") Long jobId, HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        if (!"ADMIN".equals(me.getRole())) return R.fail(403, "无权限");

        return R.ok(syncService.rerunFailedUsers(jobId, "MANUAL_RERUN"));
    }
}