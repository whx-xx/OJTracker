package hdc.rjxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.pojo.SubmissionLog;
import hdc.rjxy.pojo.vo.SubmissionTimelineVO;
import hdc.rjxy.pojo.vo.WeeklyProblemVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SubmissionLogMapper extends BaseMapper<SubmissionLog> {

    // 忽略重复的 submission_id
    @Insert("INSERT IGNORE INTO submission_log(user_id, platform_id, handle, submission_id, contest_id, problem_index, problem_name, problem_url, verdict, rating, submit_time) " +
            "VALUES (#{userId}, #{platformId}, #{handle}, #{submissionId}, #{contestId}, #{problemIndex}, #{problemName}, #{problemUrl}, #{verdict}, #{rating}, #{submitTime})")
    int insertIgnore(@Param("userId") Long userId,
                     @Param("platformId") Long platformId,
                     @Param("handle") String handle,
                     @Param("submissionId") Long submissionId,
                     @Param("contestId") Integer contestId,
                     @Param("problemIndex") String problemIndex,
                     @Param("problemName") String problemName,
                     @Param("problemUrl") String problemUrl,
                     @Param("verdict") String verdict,
                     @Param("rating") Integer rating,
                     @Param("submitTime") LocalDateTime submitTime);

    @Select("SELECT contestId, problemIndex, problemName, problemUrl, verdict, submitTime, submissionId " +
            "FROM ( " +
            "  SELECT " +
            "    contest_id AS contestId, " +
            "    problem_index AS problemIndex, " +
            "    problem_name AS problemName, " +
            "    problem_url  AS problemUrl, " +
            "    verdict      AS verdict, " +
            "    submit_time  AS submitTime, " +
            "    submission_id AS submissionId, " +
            "    ROW_NUMBER() OVER ( " +
            "      PARTITION BY handle, contest_id, problem_index " +
            "      ORDER BY " +
            "        CASE WHEN verdict = 'OK' THEN 0 ELSE 1 END ASC, " +
            "        submit_time DESC " +
            "    ) AS rn " +
            "  FROM submission_log " +
            "  WHERE user_id = #{userId} " +
            "    AND platform_id = #{platformId} " +
            "    AND handle = #{handle} " +
            "    AND submit_time >= #{start} " +
            "    AND submit_time < #{end} " +
            ") t " +
            "WHERE rn = 1 " +
            "ORDER BY submitTime DESC")
    List<WeeklyProblemVO> listWeeklyProblems(@Param("userId") Long userId,
                                             @Param("platformId") Long platformId,
                                             @Param("handle") String handle,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);
    /**
     * 统计指定时间范围内该用户的总提交数
     */
    @Select("SELECT COUNT(*) FROM submission_log " +
            "WHERE user_id = #{userId} AND platform_id = #{platformId} " +
            "AND submit_time >= #{start} AND submit_time <= #{end}")
    int countByDate(@Param("userId") Long userId,
                    @Param("platformId") Long platformId,
                    @Param("start") LocalDateTime start,
                    @Param("end") LocalDateTime end);

    /**
     * 统计指定时间范围内该用户的 AC 提交数 (Verdict = 'OK')
     */
    @Select("SELECT COUNT(*) FROM submission_log " +
            "WHERE user_id = #{userId} AND platform_id = #{platformId} " +
            "AND verdict = 'OK' " +
            "AND submit_time >= #{start} AND submit_time <= #{end}")
    int countAcceptByDate(@Param("userId") Long userId,
                          @Param("platformId") Long platformId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    /**
     * 统计指定时间范围内该用户的去重 AC 题目数
     * 根据 contest_id + problem_index 去重
     */
    @Select("SELECT COUNT(DISTINCT contest_id, problem_index) FROM submission_log " +
            "WHERE user_id = #{userId} AND platform_id = #{platformId} " +
            "AND verdict = 'OK' " +
            "AND submit_time >= #{start} AND submit_time <= #{end}")
    int countDistinctSolvedByDate(@Param("userId") Long userId,
                                  @Param("platformId") Long platformId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    /**
     * 自定义查询：获取提交时间轴，同时关联 solved_problem 表以获取标签。
     * 支持按 题目名称、题目编号、判题结果 或 标签(Tags) 进行搜索。
     */
    @Select("<script>" +
            "SELECT s.submission_id, s.contest_id, s.problem_index, s.problem_name, s.problem_url, " +
            "       s.verdict, s.rating, s.submit_time, " +
            "       sp.tags, sp.id as solvedProblemId " + // 查出 tags 和 solvedProblemId (用于前端编辑)
            "FROM submission_log s " +
            "LEFT JOIN solved_problem sp " +
            "  ON s.user_id = sp.user_id " +
            "  AND s.platform_id = sp.platform_id " +
            "  AND s.contest_id = sp.contest_id " +
            "  AND s.problem_index = sp.problem_index " +
            "WHERE s.user_id = #{userId} " +
            "  AND s.platform_id = #{platformId} " +
            "  AND s.handle = #{handle} " +

            // 动态时间筛选
            "<if test='startTime != null'> AND s.submit_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND s.submit_time &lt; #{endTime} </if>" +

            // 关键词搜索：同时匹配题目名、Verdict、Index 和 Tags
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (" +
            "    s.problem_name LIKE CONCAT('%', #{keyword}, '%') " +
            "    OR s.problem_index LIKE CONCAT('%', #{keyword}, '%') " +
            "    OR s.verdict LIKE CONCAT('%', #{keyword}, '%') " +
            "    OR sp.tags LIKE CONCAT('%', #{keyword}, '%') " + // 核心：支持按标签搜索
            "  )" +
            "</if>" +

            "ORDER BY s.submit_time DESC" +
            "</script>")
    IPage<SubmissionTimelineVO> selectTimelineWithTags(Page<?> page,
                                                       @Param("userId") Long userId,
                                                       @Param("platformId") Long platformId,
                                                       @Param("handle") String handle,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime,
                                                       @Param("keyword") String keyword);

}