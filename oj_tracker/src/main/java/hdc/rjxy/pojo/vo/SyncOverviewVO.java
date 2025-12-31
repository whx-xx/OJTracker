package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.util.List;

@Data
public class SyncOverviewVO {
    private SyncJobLogVO latestRating;
    private SyncJobLogVO latestDaily;
    private List<SyncJobLogVO> recent;
}