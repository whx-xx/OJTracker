package hdc.rjxy.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_op_log")
public class AdminOpLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminId;       // 操作者ID
    private Long targetUserId;  // 被操作对象ID（如有）

    private String opType;      // 操作类型，如"封禁用户"
    private String remark;      // 备注/详情/结果

    private LocalDateTime opTime;
}