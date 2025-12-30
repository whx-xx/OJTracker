package hdc.rjxy.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_activity")
public class DailyActivity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long platformId;
    private String handle;
    private LocalDate day;

    private Integer submitCnt;
    private Integer acceptCnt;
    private Integer solvedCnt;

    private LocalDateTime updatedAt;
}