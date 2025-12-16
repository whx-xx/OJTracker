package hdc.rjxy.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface SolvedProblemMapper {

    int insertIgnore(@Param("userId") Long userId,
                     @Param("platformId") Long platformId,
                     @Param("handle") String handle,
                     @Param("contestId") Integer contestId,
                     @Param("problemIndex") String problemIndex,
                     @Param("problemKey") String problemKey,
                     @Param("problemName") String problemName,
                     @Param("problemUrl") String problemUrl,
                     @Param("firstAcTime") LocalDateTime firstAcTime);
    // 统计：去重后的 solved 题目数（按 first_ac_time 落在区间内）
    Integer countSolvedInRange(@Param("userId") Long userId,
                               @Param("platformId") Long platformId,
                               @Param("handle") String handle,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);
}
