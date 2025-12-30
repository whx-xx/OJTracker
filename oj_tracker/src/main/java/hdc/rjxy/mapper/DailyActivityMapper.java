package hdc.rjxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hdc.rjxy.pojo.DailyActivity;
import hdc.rjxy.pojo.vo.DailyActivitySummaryVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface DailyActivityMapper extends BaseMapper<DailyActivity> {

    // 保持高效的 Upsert 逻辑
    @Insert("INSERT INTO daily_activity(user_id, platform_id, handle, day, submit_cnt, accept_cnt, solved_cnt, updated_at) " +
            "VALUES (#{userId}, #{platformId}, #{handle}, #{day}, #{submitCnt}, #{acceptCnt}, #{solvedCnt}, NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "submit_cnt = VALUES(submit_cnt), " +
            "accept_cnt = VALUES(accept_cnt), " +
            "solved_cnt = VALUES(solved_cnt), " +
            "updated_at = NOW()")
    int upsert(@Param("userId") Long userId,
               @Param("platformId") Long platformId,
               @Param("handle") String handle,
               @Param("day") LocalDate day,
               @Param("submitCnt") int submitCnt,
               @Param("acceptCnt") int acceptCnt,
               @Param("solvedCnt") int solvedCnt);

    /**
     * 统计指定日期范围内的提交总数、AC总数、活跃天数
     */
    @Select("SELECT " +
            "COALESCE(SUM(submit_cnt), 0) AS submitTotal, " +
            "COALESCE(SUM(accept_cnt), 0) AS acceptTotal, " +
            "COALESCE(SUM(solved_cnt), 0) AS solvedTotal, " +
            "COUNT(CASE WHEN submit_cnt > 0 THEN 1 END) AS activeDays " +
            "FROM daily_activity " +
            "WHERE user_id = #{userId} AND platform_id = #{platformId} " +
            "AND day >= #{start} AND day <= #{end}")
    DailyActivitySummaryVO sumByRange(@Param("userId") Long userId,
                                      @Param("platformId") Long platformId,
                                      @Param("handle") String handle, // handle 参数这里其实没用到，但为了兼容性保留
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);
}