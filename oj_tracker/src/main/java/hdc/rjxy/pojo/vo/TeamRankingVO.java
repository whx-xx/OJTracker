package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TeamRankingVO {
    private Long userId;
    private String studentNo;
    private String nickname;
    private Integer rating;
    private LocalDateTime snapshotTime;
    private String handle;
    private String avatar;
}
