package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.vo.TeamRankingVO;
import hdc.rjxy.service.TeamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/rankings")
    public R<List<TeamRankingVO>> rankings(
            @RequestParam(value = "platformCode", required = false, defaultValue = "CF") String platformCode
    ) {
        return R.ok(teamService.ranking(platformCode));
    }
}
