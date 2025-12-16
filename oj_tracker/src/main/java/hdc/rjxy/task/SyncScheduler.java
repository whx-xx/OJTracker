package hdc.rjxy.task;

import hdc.rjxy.service.SyncService;
import lombok.Data;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Data
public class SyncScheduler {

    private final SyncService syncService;

    // 开关
    private volatile boolean enabled = false;

    public SyncScheduler(SyncService syncService) {
        this.syncService = syncService;
    }

    /** 每天 02:00 / 14:00 跑一次 RATING_SYNC */
    @Scheduled(cron = "0 0 2,14 * * ?", zone = "Asia/Shanghai")
//    @Scheduled(cron = "0 */1 * * * ?") // 每分钟跑一次，测试用
    public void ratingSync() {
        if (!enabled) return;
        try {
            syncService.runCfRatingSync("MANUAL");
        } catch (Exception ignored) {
            // 这里别抛出，否则会反复刷异常；失败日志你已写入 sync_user_log
        }
    }

    /** 每 2 小时跑一次 DAILY_SYNC（只同步最近 2~7 天） */
    @Scheduled(cron = "0 0 */2 * * ?", zone = "Asia/Shanghai")
//    @Scheduled(cron = "0 */1 * * * ?") // 每分钟跑一次，测试用
    public void dailySync() {
        if (!enabled) return;
        try {
            syncService.runCfDailySync("MANUAL",3); // 传啥都行，你内部会 clamp 到 2~7
        } catch (Exception ignored) {
        }
    }
}
