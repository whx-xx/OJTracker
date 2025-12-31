package hdc.rjxy.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sync_user_log")
public class SyncUserLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long jobId;
    private Long userId;
    private Long platformId;

    private String status;      // SUCCESS, FAIL, SKIP
    private String errorCode;
    private String errorMessage;
    private LocalDateTime fetchedAt;
}