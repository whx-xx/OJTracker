package hdc.rjxy.mapper;

import hdc.rjxy.pojo.vo.SyncJobLogVO;
import hdc.rjxy.pojo.vo.SyncUserFailVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SyncLogMapper {

    int insertJob(@Param("jobType") String jobType,
                  @Param("status") String status,
                  @Param("startTime") LocalDateTime startTime,
                  @Param("triggerSource") String triggerSource);

    int updateJobFinish(@Param("id") Long id,
                        @Param("status") String status,
                        @Param("endTime") LocalDateTime endTime,
                        @Param("durationMs") Long durationMs,
                        @Param("totalCount") Integer totalCount,
                        @Param("successCount") Integer successCount,
                        @Param("failCount") Integer failCount,
                        @Param("message") String message);

    Long lastInsertId();

    int insertUserLog(@Param("jobId") Long jobId,
                      @Param("userId") Long userId,
                      @Param("platformId") Long platformId,
                      @Param("status") String status,
                      @Param("errorCode") String errorCode,
                      @Param("errorMessage") String errorMessage);

    List<SyncJobLogVO> pageJobs(@Param("offset") int offset,
                                @Param("pageSize") int pageSize,
                                @Param("jobType") String jobType);

    Long countJobs(@Param("jobType") String jobType);

    SyncJobLogVO findJob(@Param("jobId") Long jobId);

    List<SyncUserFailVO> listFailUsers(@Param("jobId") Long jobId);

    SyncJobLogVO findLatestByType(@Param("jobType") String jobType);

    List<SyncJobLogVO> listRecent(@Param("limit") int limit,
                                  @Param("jobType") String jobType);
}
