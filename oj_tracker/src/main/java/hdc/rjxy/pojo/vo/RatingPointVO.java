package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RatingPointVO {
    private LocalDateTime time;
    private Integer rating;
    private String contestName;
    private Integer delta;
    private Integer rank;
}