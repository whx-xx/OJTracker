package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.vo.TeamRankingVO;
import hdc.rjxy.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "团队相关", description = "团队榜单与管理")
@RestController
@RequestMapping("/api/team")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @Operation(summary = "获取团队榜单", description = "获取默认团队的成员 Rating 排名")
    @GetMapping("/rankings")
    public R<List<TeamRankingVO>> getRankings(
            @RequestParam(value = "platformCode", defaultValue = "CF") String platformCode
    ) {
        return R.ok(teamService.getRankings(platformCode));
    }
}