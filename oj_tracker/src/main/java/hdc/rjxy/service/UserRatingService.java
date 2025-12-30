package hdc.rjxy.service;

import hdc.rjxy.pojo.vo.RatingHistoryPointVO;
import java.util.List;

public interface UserRatingService {
    /**
     * 获取详细的 Rating 历史曲线数据，包含 Delta 计算
     */
    List<RatingHistoryPointVO> history(Long userId, String platformCode, int days);
}