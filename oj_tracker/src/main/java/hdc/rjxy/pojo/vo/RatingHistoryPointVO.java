package hdc.rjxy.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RatingHistoryPointVO {

    private LocalDateTime time;   // 比赛时间
    private Integer rating;       // newRating
    private Integer delta;        // delta 不存库，运行时算
    private String contestName;   // 比赛名
    private Integer rank;
}
