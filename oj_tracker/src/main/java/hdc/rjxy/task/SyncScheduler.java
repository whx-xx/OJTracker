package hdc.rjxy.task;

import hdc.rjxy.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    private final SyncService syncService;

    // 默认开启
    private volatile boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("定时任务状态已更新为: {}", enabled ? "开启" : "关闭");
    }

    // [新增] 获取当前状态
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 每天 02:00 / 14:00 跑一次 RATING_SYNC
     */
    @Scheduled(cron = "0 0 2,14 * * ?", zone = "Asia/Shanghai")
    public void ratingSync() {
        if (!enabled) {
            log.info("定时任务已关闭，跳过 RATING_SYNC");
            return;
        }
        log.info("Starting scheduled RATING_SYNC...");
        try {
            syncService.runCfRatingSync("SCHEDULED");
        } catch (Exception e) {
            log.error("Scheduled RATING_SYNC failed", e);
        }
    }

    /**
     * 每 2 小时跑一次 DAILY_SYNC
     */
    @Scheduled(cron = "0 0 */2 * * ?", zone = "Asia/Shanghai")
    public void dailySync() {
        if (!enabled) {
            log.info("定时任务已关闭，跳过 DAILY_SYNC");
            return;
        }
        log.info("Starting scheduled DAILY_SYNC...");
        try {
            // 同步最近3天
            syncService.runCfDailySync("SCHEDULED", 3);
        } catch (Exception e) {
            log.error("Scheduled DAILY_SYNC failed", e);
        }
    }
}