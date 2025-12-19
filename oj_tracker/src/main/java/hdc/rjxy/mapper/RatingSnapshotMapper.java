package hdc.rjxy.mapper;

import hdc.rjxy.pojo.vo.RatingPointVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RatingSnapshotMapper {

    int insert(@Param("userId") Long userId,
               @Param("platformId") Long platformId,
               @Param("handle") String handle,
               @Param("rating") int rating,
               @Param("contestName") String contestName,
               @Param("rank") Integer rank,
               @Param("snapshotTime") LocalDateTime snapshotTime);

    RatingPointVO findLast(@Param("userId") Long userId,
                           @Param("platformId") Long platformId,
                           @Param("handle") String handle);

    List<RatingPointVO> listByTimeRange(@Param("userId") Long userId,
                                        @Param("platformId") Long platformId,
                                        @Param("handle") String handle,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    RatingPointVO findFirstInRange(@Param("userId") Long userId,
                                   @Param("platformId") Long platformId,
                                   @Param("handle") String handle,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    RatingPointVO findLastInRange(@Param("userId") Long userId,
                                  @Param("platformId") Long platformId,
                                  @Param("handle") String handle,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);


}
