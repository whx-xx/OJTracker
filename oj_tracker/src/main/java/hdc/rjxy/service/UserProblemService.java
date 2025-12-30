package hdc.rjxy.service;

import hdc.rjxy.pojo.vo.WeeklyProblemVO;
import java.util.List;

public interface UserProblemService {
    /**
     * 获取用户本周（自然周）涉及的题目列表
     */
    List<WeeklyProblemVO> week(Long userId, String platformCode);
}