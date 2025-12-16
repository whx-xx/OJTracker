package hdc.rjxy.mapper;

import hdc.rjxy.pojo.vo.SubmissionTimelineVO;
import hdc.rjxy.pojo.vo.WeeklyProblemVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SubmissionLogMapper {

    int insertIgnore(@Param("userId") Long userId,
                     @Param("platformId") Long platformId,
                     @Param("handle") String handle,
                     @Param("submissionId") Long submissionId,
                     @Param("contestId") Integer contestId,
                     @Param("problemIndex") String problemIndex,
                     @Param("problemName") String problemName,
                     @Param("problemUrl") String problemUrl,
                     @Param("verdict") String verdict,
                     @Param("submitTime") LocalDateTime submitTime);

    Long findMaxSubmissionId(@Param("userId") Long userId,
                             @Param("platformId") Long platformId,
                             @Param("handle") String handle);

    List<WeeklyProblemVO> listWeeklyProblems(@Param("userId") Long userId,
                                             @Param("platformId") Long platformId,
                                             @Param("handle") String handle,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);
    LocalDateTime findLastSubmitTime(@Param("userId") Long userId,
                                     @Param("platformId") Long platformId,
                                     @Param("handle") String handle);
    List<SubmissionTimelineVO> listTimeline(@Param("userId") Long userId,
                                            @Param("platformId") Long platformId,
                                            @Param("handle") String handle,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("limit") int limit);
}
