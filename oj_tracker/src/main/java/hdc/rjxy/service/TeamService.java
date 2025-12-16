package hdc.rjxy.service;

import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.TeamRankingMapper;
import hdc.rjxy.pojo.vo.TeamRankingVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final PlatformMapper platformMapper;
    private final TeamRankingMapper teamRankingMapper;

    public TeamService(PlatformMapper platformMapper, TeamRankingMapper teamRankingMapper) {
        this.platformMapper = platformMapper;
        this.teamRankingMapper = teamRankingMapper;
    }

    public List<TeamRankingVO> ranking(String platformCode) {
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";
        Long pid = platformMapper.findIdByCode(platformCode);
        if (pid == null) throw new IllegalArgumentException("平台不存在: " + platformCode);

        return teamRankingMapper.listRanking("DEFAULT", pid);
    }
}
