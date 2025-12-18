package hdc.rjxy.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserStatsVO {
    private Long totalUsers;
    private Long adminCount;
    private Long activeCount;
    private Long bannedCount;
}