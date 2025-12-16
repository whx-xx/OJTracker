package hdc.rjxy.mapper;

import hdc.rjxy.pojo.vo.DailyActivitySummaryVO;
import hdc.rjxy.pojo.vo.HeatmapDayVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DailyActivityMapper {
    List<HeatmapDayVO> listHeatmap(@Param("userId") Long userId,
                                   @Param("platformId") Long platformId,
                                   @Param("handle") String handle,
                                   @Param("startDay") LocalDate startDay,
                                   @Param("endDay") LocalDate endDay);

    DailyActivitySummaryVO sumByRange(@Param("userId") Long userId,
                                      @Param("platformId") Long platformId,
                                      @Param("handle") String handle,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);

    int upsert(@Param("userId") Long userId,
               @Param("platformId") Long platformId,
               @Param("handle") String handle,
               @Param("day") LocalDate day,
               @Param("submitCnt") int submitCnt,
               @Param("acceptCnt") int acceptCnt,
               @Param("solvedCnt") int solvedCnt);

    LocalDate findLastDay(@Param("userId") Long userId,
                          @Param("platformId") Long platformId,
                          @Param("handle") String handle);
}
