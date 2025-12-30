package hdc.rjxy.service;

import hdc.rjxy.pojo.vo.TeamRankingVO;
import java.util.List;

public interface TeamService {
    List<TeamRankingVO> getRankings(String platformCode);
}