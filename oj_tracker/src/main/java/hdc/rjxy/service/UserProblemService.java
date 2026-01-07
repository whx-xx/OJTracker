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

    /**
     * 更新题目笔记
     * @param userId 当前登录用户ID (用于鉴权)
     * @param id solved_problem 主键ID
     * @param notes 笔记内容
     */
    void updateNotes(Long userId, Long id, String notes);
}