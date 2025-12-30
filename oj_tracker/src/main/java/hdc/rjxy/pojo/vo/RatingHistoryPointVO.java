package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RatingHistoryPointVO {
    private LocalDateTime time;
    private Integer rating;
    private Integer delta;
    private String contestName;
    private Integer rank;
}