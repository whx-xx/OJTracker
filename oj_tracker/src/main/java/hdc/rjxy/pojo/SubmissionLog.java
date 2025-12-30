package hdc.rjxy.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("submission_log")
public class SubmissionLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long platformId;
    private String handle;

    private Long submissionId;
    private Integer contestId;
    private String problemIndex;
    private String problemName;
    private String problemUrl;
    private String verdict;
    private LocalDateTime submitTime;
}