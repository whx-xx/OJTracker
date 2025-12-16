package hdc.rjxy.mapper;

import hdc.rjxy.pojo.vo.TeamRankingVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface TeamRankingMapper {
    List<TeamRankingVO> listRanking(@Param("teamCode") String teamCode,
                                    @Param("platformId") Long platformId);
}
