package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SyncJobLogVO {
    private Long id;
    private String jobType;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String message;
    private String triggerSource;
}
