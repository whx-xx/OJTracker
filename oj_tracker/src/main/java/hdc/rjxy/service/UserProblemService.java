package hdc.rjxy.service;

import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.SubmissionLogMapper;
import hdc.rjxy.mapper.UserPlatformAccountMapper;
import hdc.rjxy.pojo.vo.WeeklyProblemVO;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
public class UserProblemService {

    private final PlatformMapper platformMapper;
    private final UserPlatformAccountMapper upaMapper;
    private final SubmissionLogMapper submissionLogMapper;

    public UserProblemService(PlatformMapper platformMapper,
                              UserPlatformAccountMapper upaMapper,
                              SubmissionLogMapper submissionLogMapper) {
        this.platformMapper = platformMapper;
        this.upaMapper = upaMapper;
        this.submissionLogMapper = submissionLogMapper;
    }

    public List<WeeklyProblemVO> week(Long userId, String platformCode) {
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";
        Long pid = platformMapper.findIdByCode(platformCode);
        if (pid == null) throw new IllegalArgumentException("平台不存在");

        String handle = upaMapper.findIdentifierValue(userId, pid);
        if (handle == null || handle.isBlank()) throw new IllegalArgumentException("未绑定平台账号");
        handle = handle.trim();

        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate nextMonday = monday.plusDays(7);

        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end = nextMonday.atStartOfDay(); // [start, end)

        return submissionLogMapper.listWeeklyProblems(userId, pid, handle, start, end);
    }
}
