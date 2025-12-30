package hdc.rjxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.SubmissionLogMapper;
import hdc.rjxy.mapper.UserPlatformAccountMapper;
import hdc.rjxy.pojo.Platform;
import hdc.rjxy.pojo.UserPlatformAccount;
import hdc.rjxy.pojo.vo.WeeklyProblemVO;
import hdc.rjxy.service.UserProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class UserProblemServiceImpl implements UserProblemService {

    @Autowired
    private PlatformMapper platformMapper;
    @Autowired
    private UserPlatformAccountMapper upaMapper;
    @Autowired
    private SubmissionLogMapper submissionLogMapper;

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
}