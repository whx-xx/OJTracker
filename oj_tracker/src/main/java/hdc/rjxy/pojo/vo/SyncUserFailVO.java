package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SyncUserFailVO {
    private Long userId;
    private String studentNo;
    private String nickname;
    private Long platformId;
    private String platformCode;
    private String platformName;
    private String status;         // FAIL / SKIP
    private String errorCode;
    private String errorMessage;
    private LocalDateTime fetchedAt;
    // 给前端展示的解释
    private String errorCodeDesc;
    private String suggestAction; // e.g. BIND_HANDLE / FIX_HANDLE / RETRY / NONE
    private Boolean retryable;
}
