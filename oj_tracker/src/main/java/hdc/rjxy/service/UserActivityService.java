package hdc.rjxy.service;

import hdc.rjxy.pojo.vo.HeatmapDayVO;
import java.util.List;

public interface UserActivityService {
    /**
     * 获取指定天数内的热力图数据
     */
    List<HeatmapDayVO> heatmap(Long userId, String platformCode, int days);
}