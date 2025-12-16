package hdc.rjxy.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminOpLogVO {
    private Long id;
    private Long adminId;
    private String adminName;     // join user.username
    private Long targetUserId;
    private String targetName;    // join user.username/studentNo
    private String opType;
    private LocalDateTime opTime;
    private String ip;
    private String remark;
}
