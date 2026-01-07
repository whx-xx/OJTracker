package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SubmissionTimelineVO {
    private Long submissionId;
    private Long userId;
    private String handle;
    private Long platformId;
    private String platformName;

    private Integer contestId;
    private String problemIndex;
    private String problemName;
    private String problemUrl;

    private String verdict;          // OK, WRONG_ANSWER, etc.
    private Integer rating;
    private LocalDateTime submitTime;

    private Long solvedProblemId;
    private String tags;

    private String notes;
}