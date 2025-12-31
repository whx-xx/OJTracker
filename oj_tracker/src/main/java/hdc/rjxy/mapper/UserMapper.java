package hdc.rjxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hdc.rjxy.pojo.User;
import hdc.rjxy.pojo.vo.TeamRankingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 继承 BaseMapper，泛型指定为实体类 User
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    /**
     * 查询全局用户排行榜
     * 直接查 user 表，左连接最新的 rating_snapshot
     */
    @Select("SELECT " +
            "  u.id AS userId, " +
            "  u.student_no AS studentNo, " +
            "  u.nickname AS nickname, " +
            "  rs.rating AS rating, " +
            "  rs.snapshot_time AS snapshotTime, " +
            "  rs.handle AS handle " +
            "FROM user u " +
            "LEFT JOIN rating_snapshot rs ON rs.user_id = u.id " +
            "  AND rs.platform_id = #{platformId} " +
            "  AND rs.snapshot_time = ( " +
            "      SELECT MAX(snapshot_time) FROM rating_snapshot sub " +
            "      WHERE sub.user_id = rs.user_id AND sub.platform_id = #{platformId} " +
            "  ) " +
            "WHERE u.status = 1 " + // 只看启用的用户
            "ORDER BY " +
            "  (rs.rating IS NULL), " + // 没分数的排后面
            "  rs.rating DESC, " +      // 分数高的排前面
            "  u.id ASC")
    List<TeamRankingVO> selectGlobalRankings(@Param("platformId") Long platformId);
}