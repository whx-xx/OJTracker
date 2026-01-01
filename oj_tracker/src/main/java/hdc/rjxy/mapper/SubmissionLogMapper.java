package hdc.rjxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hdc.rjxy.pojo.SubmissionLog;
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
    @Insert("INSERT IGNORE INTO submission_log(user_id, platform_id, handle, submission_id, contest_id, problem_index, problem_name, problem_url, verdict, submit_time) " +
            "VALUES (#{userId}, #{platformId}, #{handle}, #{submissionId}, #{contestId}, #{problemIndex}, #{problemName}, #{problemUrl}, #{verdict}, #{submitTime})")
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
}