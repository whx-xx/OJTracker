package hdc.rjxy.service;

import hdc.rjxy.cf.CfClientException;
import hdc.rjxy.cf.CfUserStatusResponse;
import hdc.rjxy.cf.CodeforcesClient;
import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.SubmissionLogMapper;
import hdc.rjxy.mapper.UserPlatformAccountMapper;
import hdc.rjxy.pojo.vo.RefreshResultVO;
import hdc.rjxy.pojo.vo.SubmissionTimelineVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
public class UserSubmissionService {

    private final PlatformMapper platformMapper;
    private final UserPlatformAccountMapper upaMapper;
    private final SubmissionLogMapper submissionLogMapper;
    private final CodeforcesClient cfClient;

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    public UserSubmissionService(PlatformMapper platformMapper,
                                 UserPlatformAccountMapper upaMapper,
                                 SubmissionLogMapper submissionLogMapper,
                                 CodeforcesClient cfClient) {
        this.platformMapper = platformMapper;
        this.upaMapper = upaMapper;
        this.submissionLogMapper = submissionLogMapper;
        this.cfClient = cfClient;
    }

    /** 时间线：纯读库 */
    public List<SubmissionTimelineVO> timeline(Long userId, String platformCode, String range, Integer limit) {
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";
        Long pid = platformMapper.findIdByCode(platformCode);
        if (pid == null) throw new IllegalArgumentException("平台不存在");

        String handle = upaMapper.findIdentifierValue(userId, pid);
        if (handle == null || handle.isBlank()) throw new IllegalArgumentException("未绑定平台账号");
        handle = handle.trim();

        int lim = (limit == null || limit <= 0) ? 50 : Math.min(limit, 200);

        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime start;
        LocalDateTime end;

        if ("TODAY".equalsIgnoreCase(range)) {
            start = today.atStartOfDay();
            end = today.plusDays(1).atStartOfDay();
        } else {
            // 默认 WEEK：周一到下周一 [start, end)
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            start = monday.atStartOfDay();
            end = monday.plusDays(7).atStartOfDay();
        }

        return submissionLogMapper.listTimeline(userId, pid, handle, start, end, lim);
    }

    /**
     * 打开页面轻量补一次：
     * - 只拉最新 count 条
     * - 增量：只插入 submit_time > lastSubmitTime 的记录（再加 INSERT IGNORE 双保险）
     */
    @Transactional
    public RefreshResultVO refreshLight(Long userId, String platformCode, Integer count) {
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";
        Long pid = platformMapper.findIdByCode(platformCode);
        if (pid == null) throw new IllegalArgumentException("平台不存在");

        String handle = upaMapper.findIdentifierValue(userId, pid);
        if (handle == null || handle.isBlank()) throw new IllegalArgumentException("未绑定平台账号");
        handle = handle.trim();

        int cnt = (count == null || count <= 0) ? 200 : Math.min(count, 400);

        LocalDateTime lastTime = submissionLogMapper.findLastSubmitTime(userId, pid, handle);

        List<CfUserStatusResponse.Submission> subs = cfClient.getUserStatus(handle, 1, cnt);

        int inserted = 0;
        for (CfUserStatusResponse.Submission s : subs) {
            Long sec = s.getCreationTimeSeconds();
            if (sec == null) continue;

            LocalDateTime submitTime = Instant.ofEpochSecond(sec).atZone(ZONE).toLocalDateTime();
            if (lastTime != null && !submitTime.isAfter(lastTime)) {
                // user.status 通常最新在前，到了旧数据可以停
                break;
            }

            Long submissionId = s.getId(); // 你 CfUserStatusResponse.Submission 需要有 id 字段
            if (submissionId == null) continue;

            Integer contestId = (s.getProblem() == null) ? null : s.getProblem().getContestId();
            String idx = (s.getProblem() == null) ? null : s.getProblem().getIndex();
            String name = (s.getProblem() == null) ? null : s.getProblem().getName();
            String url = (contestId != null && idx != null)
                    ? ("https://codeforces.com/contest/" + contestId + "/problem/" + idx)
                    : null;

            String verdict = s.getVerdict();

            int rows = submissionLogMapper.insertIgnore(
                    userId, pid, handle,
                    submissionId,
                    contestId, idx, name, url,
                    verdict, submitTime
            );
            if (rows > 0) inserted++;
        }

        RefreshResultVO vo = new RefreshResultVO();
        vo.setFetched(subs == null ? 0 : subs.size());
        vo.setInserted(inserted);
        return vo;
    }
}
