package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SubmissionTimelineVO {
    private Long submissionId;

    private Integer contestId;
    private String problemIndex;
    private String problemName;
    private String problemUrl;

    private String verdict;          // AC / WRONG_ANSWER / ...
    private LocalDateTime submitTime;
}
