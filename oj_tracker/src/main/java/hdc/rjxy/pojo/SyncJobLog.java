package hdc.rjxy.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sync_job_log")
public class SyncJobLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String jobType;      // RATING_SYNC, DAILY_SYNC
    private String status;       // RUNNING, SUCCESS, FAIL, PARTIAL_FAIL
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;

    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;

    private String message;
    private String triggerSource; // MANUAL, SCHEDULED
    private LocalDateTime createdAt;
}