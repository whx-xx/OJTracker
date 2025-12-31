package hdc.rjxy.task;

import hdc.rjxy.service.SyncService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    private final SyncService syncService;

    @Setter
    private volatile boolean enabled = true;

    /** * 每天 02:00 / 14:00 跑一次 RATING_SYNC
     */
    @Scheduled(cron = "0 0 2,14 * * ?", zone = "Asia/Shanghai")
    public void ratingSync() {
        if (!enabled) return;
        log.info("Starting scheduled RATING_SYNC...");
        try {
            syncService.runCfRatingSync("SCHEDULED");
        } catch (Exception e) {
            log.error("Scheduled RATING_SYNC failed", e);
        }
    }

    /** * 每 2 小时跑一次 DAILY_SYNC（只同步最近 3 天，确保覆盖遗漏）
     */
    @Scheduled(cron = "0 0 */2 * * ?", zone = "Asia/Shanghai")
    public void dailySync() {
        if (!enabled) return;
        log.info("Starting scheduled DAILY_SYNC...");
        try {
            syncService.runCfDailySync("SCHEDULED", 3);
        } catch (Exception e) {
            log.error("Scheduled DAILY_SYNC failed", e);
        }
    }
}