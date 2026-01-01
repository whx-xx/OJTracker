package hdc.rjxy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.pojo.vo.RefreshResultVO;
import hdc.rjxy.pojo.vo.SubmissionTimelineVO;


public interface UserSubmissionService {

    /**
     * 获取提交时间线（分页）
     * @param userId 用户ID
     * @param platformCode 平台代码
     * @param range 时间范围 (TODAY, WEEK, MONTH, ALL)
     * @param pageNum 当前页
     * @param pageSize 页大小
     */
    Page<SubmissionTimelineVO> timeline(Long userId, String platformCode, String range, String keyword, int pageNum, int pageSize);

    /**
     * 轻量级同步（手动刷新数据）
     */
    RefreshResultVO refreshLight(Long userId, String platformCode, Integer count);
}