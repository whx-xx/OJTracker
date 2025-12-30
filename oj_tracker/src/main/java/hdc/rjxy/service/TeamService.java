package hdc.rjxy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.UserMapper;
import hdc.rjxy.pojo.Platform;
import hdc.rjxy.pojo.vo.TeamRankingVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    @Autowired
    private UserMapper userMapper; // 直接注入 UserMapper

    @Autowired
    private PlatformMapper platformMapper;

    /**
     * 获取全局排行榜 (原 getRankings)
     */
    public List<TeamRankingVO> getRankings(String platformCode) {
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";

        Platform p = platformMapper.selectOne(new LambdaQueryWrapper<Platform>().eq(Platform::getCode, platformCode));
        if (p == null) throw new IllegalArgumentException("平台不存在");

        // 直接调用 UserMapper 查询全局榜单
        return userMapper.selectGlobalRankings(p.getId());
    }

    // 删除 joinDefaultTeam 方法，因为不再需要加入团队
    // 删除 ensureDefaultTeamExists 方法
}