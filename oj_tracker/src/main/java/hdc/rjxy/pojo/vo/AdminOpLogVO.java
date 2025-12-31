package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminOpLogVO {
    private Long id;

    private Long adminId;
    private String adminName;      // 补充：管理员昵称

    private Long targetUserId;
    private String targetUserName; // 补充：被操作用户昵称

    private String opType;
    private String remark;
    private LocalDateTime opTime;
}