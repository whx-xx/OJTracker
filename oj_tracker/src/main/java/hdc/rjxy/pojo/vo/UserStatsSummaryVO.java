package hdc.rjxy.pojo.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserStatsSummaryVO {
    private String platformCode;
    private int days;
    private LocalDate from;
    private LocalDate to;

    private int submitTotal;
    private int acceptTotal;
    private int solvedTotal;
    private int weeklySolved;
    private int activeDays;
    private double avgSubmitPerDay;

    private Integer ratingStart;
    private Integer ratingEnd;
    private Integer ratingDelta;

    private String lastContestName;
    private LocalDateTime lastContestTime;
}
