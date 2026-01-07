package hdc.rjxy.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("solved_problem")
public class SolvedProblem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long platformId;
    private String handle;

    private Integer contestId;
    private String problemIndex;
    private String problemKey; // 如 "1234_A"
    private String problemName;
    private String problemUrl;
    private Integer rating;
    private LocalDateTime firstAcTime;

    private String tags;
    private String notes;
}