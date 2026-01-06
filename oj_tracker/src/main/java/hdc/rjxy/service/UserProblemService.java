package hdc.rjxy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.pojo.SolvedProblem;
import hdc.rjxy.pojo.vo.WeeklyProblemVO;
import java.util.List;

public interface UserProblemService {
    /**
     * 获取用户本周（自然周）涉及的题目列表
     */
    List<WeeklyProblemVO> week(Long userId, String platformCode);

    void updateTags(Long userId, Long problemId, List<String> tags);
}