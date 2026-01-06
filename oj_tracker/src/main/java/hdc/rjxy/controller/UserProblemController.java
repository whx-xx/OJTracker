package hdc.rjxy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.common.R;
import hdc.rjxy.pojo.SolvedProblem;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.dto.UpdateProblemTagsReq;
import hdc.rjxy.pojo.vo.WeeklyProblemVO;
import hdc.rjxy.service.UserProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "近期题目", description = "本周做题列表")
@RestController
@RequestMapping("/api/user/problems")
public class UserProblemController {

    private final UserProblemService userProblemService;

    public UserProblemController(UserProblemService userProblemService) {
        this.userProblemService = userProblemService;
    }

    @Operation(summary = "获取本周题目")
    @GetMapping("/week")
    public R<List<WeeklyProblemVO>> week(@RequestParam(value = "platformCode", defaultValue = "CF") String platformCode,
                                         HttpSession session) {
        UserSession me = (UserSession) session.getAttribute("user");
        if (me == null) return R.fail(401, "未登录");

        return R.ok(userProblemService.week(me.getId(), platformCode));
    }

    @Operation(summary = "更新题目标签")
    @PostMapping("/update-tags")
    public R<Void> updateTags(@RequestBody UpdateProblemTagsReq req, HttpSession session) {
        UserSession me = (UserSession) session.getAttribute("user");
        if (me == null) return R.fail(401, "未登录");

        userProblemService.updateTags(me.getId(), req.getId(), req.getTags());
        return R.ok();
    }
}