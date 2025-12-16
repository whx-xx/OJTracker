package hdc.rjxy.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RatingSnapshotPointVO {
    private LocalDateTime time;   // snapshot_time
    private Integer rating;       // rating
    private Integer maxRating;    // max_rating
    private Integer delta;        // 和上一个点的差值（没有就 0）
}
