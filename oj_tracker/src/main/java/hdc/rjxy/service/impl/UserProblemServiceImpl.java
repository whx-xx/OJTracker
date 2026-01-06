package hdc.rjxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.SolvedProblemMapper;
import hdc.rjxy.mapper.SubmissionLogMapper;
import hdc.rjxy.mapper.UserPlatformAccountMapper;
import hdc.rjxy.pojo.Platform;
import hdc.rjxy.pojo.SolvedProblem;
import hdc.rjxy.pojo.UserPlatformAccount;
import hdc.rjxy.pojo.vo.WeeklyProblemVO;
import hdc.rjxy.service.UserProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserProblemServiceImpl implements UserProblemService {

    @Autowired
    private PlatformMapper platformMapper;
    @Autowired
    private UserPlatformAccountMapper upaMapper;
    @Autowired
    private SubmissionLogMapper submissionLogMapper;
    @Autowired
    private SolvedProblemMapper solvedProblemMapper;

    @Override
    public List<WeeklyProblemVO> week(Long userId, String platformCode) {
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";
        Platform p = platformMapper.selectOne(new LambdaQueryWrapper<Platform>().eq(Platform::getCode, platformCode));
        if (p == null) throw new IllegalArgumentException("平台不存在");

        UserPlatformAccount account = upaMapper.selectOne(new LambdaQueryWrapper<UserPlatformAccount>()
                .eq(UserPlatformAccount::getUserId, userId)
                .eq(UserPlatformAccount::getPlatformId, p.getId()));
        if (account == null) throw new IllegalArgumentException("未绑定平台账号");

        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);

        // 计算本周一开始时间
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end = start.plusDays(7); // 到下周一

        return submissionLogMapper.listWeeklyProblems(userId, p.getId(), account.getIdentifierValue(), start, end);
    }

    @Override
    public void updateTags(Long userId, Long problemId, List<String> tags) {
        // 1. 校验题目是否存在且属于该用户
        SolvedProblem problem = solvedProblemMapper.selectById(problemId);
        if (problem == null) {
            throw new IllegalArgumentException("题目记录不存在");
        }
        if (!problem.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改他人的题目记录");
        }

        // 2. 处理标签格式
        String tagsStr = "";
        if (tags != null && !tags.isEmpty()) {
            // 去除空白标签并用逗号连接
            tagsStr = tags.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
        }

        // 3. 更新数据库
        problem.setTags(tagsStr);
        solvedProblemMapper.updateById(problem);
    }

}