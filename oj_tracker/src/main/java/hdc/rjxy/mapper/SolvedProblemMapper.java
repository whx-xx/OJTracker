package hdc.rjxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hdc.rjxy.pojo.SolvedProblem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface SolvedProblemMapper extends BaseMapper<SolvedProblem> {

    @Insert("INSERT IGNORE INTO solved_problem(user_id, platform_id, handle, contest_id, problem_index, problem_key, problem_name, problem_url, rating, first_ac_time) " +
            "VALUES (#{userId}, #{platformId}, #{handle}, #{contestId}, #{problemIndex}, #{problemKey}, #{problemName}, #{problemUrl}, #{rating}, #{firstAcTime})")
    int insertIgnore(@Param("userId") Long userId,
                     @Param("platformId") Long platformId,
                     @Param("handle") String handle,
                     @Param("contestId") Integer contestId,
                     @Param("problemIndex") String problemIndex,
                     @Param("problemKey") String key,
                     @Param("problemName") String name,
                     @Param("problemUrl") String url,
                     @Param("rating") Integer rating,
                     @Param("firstAcTime") LocalDateTime time);

    @Select("SELECT COUNT(*) FROM solved_problem " +
            "WHERE user_id = #{userId} AND platform_id = #{platformId} " +
            "AND handle = #{handle} " +
            "AND first_ac_time >= #{start} AND first_ac_time < #{end}")
    int countSolvedInRange(@Param("userId") Long userId,
                           @Param("platformId") Long platformId,
                           @Param("handle") String handle,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    /**
     * 统计各 Rating 的解题数量
     * 返回格式示例: [{rating=800, cnt=10}, {rating=900, cnt=5}, ...]
     */
    @Select("SELECT rating, COUNT(*) as cnt FROM solved_problem " +
            "WHERE user_id = #{userId} AND platform_id = #{platformId} " +
            "AND rating IS NOT NULL " +
            "GROUP BY rating " +
            "ORDER BY rating ASC")
    List<Map<String, Object>> countSolvedByRating(@Param("userId") Long userId,
                                                  @Param("platformId") Long platformId);
}