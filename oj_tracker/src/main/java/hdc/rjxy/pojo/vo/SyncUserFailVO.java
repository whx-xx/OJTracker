package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SyncUserFailVO {
    private Long userId;
    private String handle; // 需要从关联查询中获取
    private String status;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime fetchedAt;

    // 错误字典描述（可选，前端展示用）
    private String errorCodeDesc;
    private String suggestAction;
    private boolean retryable;
}