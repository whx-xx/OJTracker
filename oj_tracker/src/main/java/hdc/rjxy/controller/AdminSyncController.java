package hdc.rjxy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.aop.LogAdminOp;
import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.vo.SyncJobDetailVO;
import hdc.rjxy.pojo.vo.SyncJobLogVO;
import hdc.rjxy.pojo.vo.SyncOverviewVO;
import hdc.rjxy.service.SyncService;
import hdc.rjxy.task.SyncScheduler;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
public class AdminSyncController {

    private final SyncService syncService;
    private final SyncScheduler syncScheduler;

    // 1. 任务列表
    @GetMapping("/jobs")
    public R<Page<SyncJobLogVO>> jobs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "jobType", required = false) String jobType,
            HttpSession session
    ) {
        checkAdmin(session);
        return R.ok(syncService.pageJobs(page, pageSize, jobType));
    }

    // 2. 任务详情
    @GetMapping("/jobs/{jobId}")
    public R<SyncJobDetailVO> jobDetail(@PathVariable("jobId") Long jobId, HttpSession session) {
        checkAdmin(session);
        return R.ok(syncService.jobDetail(jobId));
    }

    // 3. 手动触发同步
    @LogAdminOp("手动触发同步任务")
    @PostMapping("/run")
    public R<Long> run(@RequestParam("jobType") String jobType,
                       @RequestParam(value = "days", required = false) Integer days,
                       HttpSession session) {
        checkAdmin(session);

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
        checkAdmin(session);
        return R.ok(syncService.overview(limit));
    }

    // 5. 重跑失败任务
    @LogAdminOp("重跑失败同步任务")
    @PostMapping("/rerun")
    public R<Long> rerun(@RequestParam("jobId") Long jobId, HttpSession session) {
        checkAdmin(session);
        return R.ok(syncService.rerunFailedUsers(jobId, "MANUAL_RERUN"));
    }

    // 6. 获取定时任务开关状态
    @GetMapping("/schedule/status")
    public R<Boolean> getScheduleStatus(HttpSession session) {
        checkAdmin(session);
        return R.ok(syncScheduler.isEnabled());
    }

    // 7. 切换定时任务开关
    @LogAdminOp("切换定时任务状态")
    @PostMapping("/schedule/toggle")
    public R<Boolean> toggleSchedule(@RequestParam("enabled") Boolean enabled, HttpSession session) {
        checkAdmin(session);
        if (enabled == null) return R.fail("状态不能为空");

        syncScheduler.setEnabled(enabled);
        return R.ok(syncScheduler.isEnabled());
    }

    private void checkAdmin(HttpSession session) {
        UserSession me = (UserSession) session.getAttribute("user");
        if (me == null) throw new RuntimeException("未登录");
        // 如果使用了统一异常处理，这里直接抛出异常即可；否则需要返回 R.fail。
        // 为了代码简洁，这里假设有 GlobalExceptionHandler 处理运行时异常，或者在 AuthInterceptor 层已经拦截。
        // 如果没有拦截器强制拦截，这里应手动抛出异常让全局捕获，或改为返回 R 对象。
        if (!"ADMIN".equals(me.getRole())) {
            throw new RuntimeException("无权限");
        }
    }
}