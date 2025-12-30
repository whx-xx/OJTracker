package hdc.rjxy.service;

import hdc.rjxy.pojo.vo.RatingPointVO;
import hdc.rjxy.pojo.vo.UserStatsSummaryVO;

import java.util.List;

public interface UserStatsService {
    /**
     * 获取 Rating 历史数据（用于图表或简略展示）
     */
    List<RatingPointVO> ratingHistory(Long userId, String platformCode, Integer limit);

    /**
     * 获取个人主页的数据汇总（提交数、AC数、本周解题、Rating概览）
     */
    UserStatsSummaryVO summary(Long userId, String platformCode, int days);
}