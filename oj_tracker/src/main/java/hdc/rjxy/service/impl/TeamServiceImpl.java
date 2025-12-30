package hdc.rjxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.UserMapper;
import hdc.rjxy.pojo.Platform;
import hdc.rjxy.pojo.vo.TeamRankingVO;
import hdc.rjxy.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamServiceImpl implements TeamService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PlatformMapper platformMapper;

    @Override
    public List<TeamRankingVO> getRankings(String platformCode) {
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";
        Platform p = platformMapper.selectOne(new LambdaQueryWrapper<Platform>().eq(Platform::getCode, platformCode));
        if (p == null) throw new IllegalArgumentException("平台不存在");

        return userMapper.selectGlobalRankings(p.getId());
    }
}