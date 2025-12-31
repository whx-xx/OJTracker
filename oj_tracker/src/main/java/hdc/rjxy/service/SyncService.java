package hdc.rjxy.service;

import hdc.rjxy.pojo.vo.SyncJobDetailVO;
import hdc.rjxy.pojo.vo.SyncJobLogVO;
import hdc.rjxy.pojo.vo.SyncOverviewVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface SyncService {

    /**
     * 执行 Codeforces Rating 同步任务
     * @param triggerSource 触发源 (MANUAL/SCHEDULED)
     * @return 任务ID
     */
    Long runCfRatingSync(String triggerSource);

    /**
     * 执行 Codeforces 每日刷题记录同步任务
     * @param triggerSource 触发源
     * @param days 回溯天数
     * @return 任务ID
     */
    Long runCfDailySync(String triggerSource, int days);

    // --- 管理端功能 ---
    Page<SyncJobLogVO> pageJobs(int page, int pageSize, String jobType);

    SyncJobDetailVO jobDetail(Long jobId);

    SyncOverviewVO overview(int recentLimit);

    Long rerunFailedUsers(Long oldJobId, String triggerSource);
}