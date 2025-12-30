package hdc.rjxy.service;

import hdc.rjxy.pojo.vo.RefreshResultVO;
import hdc.rjxy.pojo.vo.SubmissionTimelineVO;

import java.util.List;

public interface UserSubmissionService {
    /**
     * 获取提交记录时间线
     */
    List<SubmissionTimelineVO> timeline(Long userId, String platformCode, String range, Integer limit);

    /**
     * 轻量级同步（手动刷新数据）
     */
    RefreshResultVO refreshLight(Long userId, String platformCode, Integer count);
}