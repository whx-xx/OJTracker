package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RatingPointVO {
    private LocalDateTime time;      // snapshot_time
    private Integer rating;          // rating
    private String contestName;      // contest_name
    private Integer delta;           // delta
    private Integer rank;
}
