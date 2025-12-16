package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WeeklyProblemVO {
    private Integer contestId;
    private String problemIndex;
    private String problemName;
    private String problemUrl;

    private String verdict;          // OK / WRONG_ANSWER / ...
    private LocalDateTime submitTime;
    private Long submissionId;
}
