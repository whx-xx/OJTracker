package hdc.rjxy.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rating_snapshot")
public class RatingSnapshot {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long platformId;
    private String handle;

    private Integer rating;
    private String contestName;
    private Integer contestRank;
    private LocalDateTime snapshotTime;
}