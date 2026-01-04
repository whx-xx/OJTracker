package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.util.List;

/**
 * 任务详情聚合对象
 */
@Data
public class SyncJobDetailVO {
    // 任务基本信息
    private SyncJobLogVO job;

    // 失败用户列表
    private List<SyncUserFailVO> failList;

    // 变更用户列表（成功且有新数据的用户）
    private List<SyncUserChangeVO> changeList;
}