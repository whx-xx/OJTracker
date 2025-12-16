package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.util.List;

@Data
public class SyncJobDetailVO {
    private SyncJobLogVO job;
    private List<SyncUserFailVO> failList;
}
