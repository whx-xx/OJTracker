package hdc.rjxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.cf.CfUserRatingResponse;
import hdc.rjxy.cf.CfUserStatusResponse;
import hdc.rjxy.cf.CodeforcesClient;
import hdc.rjxy.mapper.*;
import hdc.rjxy.pojo.*;
import hdc.rjxy.pojo.vo.RefreshResultVO;
import hdc.rjxy.pojo.vo.SubmissionTimelineVO;
import hdc.rjxy.service.UserSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserSubmissionServiceImpl implements UserSubmissionService {

    @Autowired
    private PlatformMapper platformMapper;
    @Autowired
    private UserPlatformAccountMapper upaMapper;
    @Autowired
    private SubmissionLogMapper submissionLogMapper;
    @Autowired
    private SolvedProblemMapper solvedProblemMapper;
    @Autowired
    private DailyActivityMapper dailyActivityMapper;
    @Autowired
    private RatingSnapshotMapper ratingSnapshotMapper;
    @Autowired
    private CodeforcesClient cfClient;

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Override
    public Page<SubmissionTimelineVO> timeline(Long userId, String platformCode, String range, String keyword, int pageNum, int pageSize) {
        // 1. 基础校验与平台获取
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";
        Platform p = platformMapper.selectOne(new LambdaQueryWrapper<Platform>().eq(Platform::getCode, platformCode));

        Page<SubmissionTimelineVO> emptyPage = new Page<>(pageNum, pageSize);
        if (p == null) return emptyPage;

        // 2. 获取用户绑定的 Handle
        UserPlatformAccount account = upaMapper.selectOne(new LambdaQueryWrapper<UserPlatformAccount>()
                .eq(UserPlatformAccount::getUserId, userId)
                .eq(UserPlatformAccount::getPlatformId, p.getId()));

        if (account == null) return emptyPage;
        String handle = account.getIdentifierValue();

        // 3. 构建查询条件
        LambdaQueryWrapper<SubmissionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubmissionLog::getUserId, userId)
                .eq(SubmissionLog::getPlatformId, p.getId())
                .eq(SubmissionLog::getHandle, handle);

        LocalDate today = LocalDate.now(ZONE);

        // --- 时间筛选逻辑 ---
        if ("TODAY".equalsIgnoreCase(range)) {
            wrapper.ge(SubmissionLog::getSubmitTime, today.atStartOfDay())
                    .lt(SubmissionLog::getSubmitTime, today.plusDays(1).atStartOfDay());
        } else if ("WEEK".equalsIgnoreCase(range)) {
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            wrapper.ge(SubmissionLog::getSubmitTime, monday.atStartOfDay())
                    .lt(SubmissionLog::getSubmitTime, monday.plusDays(7).atStartOfDay());
        } else if ("MONTH".equalsIgnoreCase(range)) {
            LocalDate firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate firstDayOfNextMonth = today.with(TemporalAdjusters.firstDayOfNextMonth());
            wrapper.ge(SubmissionLog::getSubmitTime, firstDayOfMonth.atStartOfDay())
                    .lt(SubmissionLog::getSubmitTime, firstDayOfNextMonth.atStartOfDay());
        }

        // --- [新增] 关键词搜索逻辑 ---
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            // 搜索 ProblemName 或 Verdict 或 ProblemIndex (如 "A", "B")
            wrapper.and(w -> w.like(SubmissionLog::getProblemName, k)
                    .or()
                    .like(SubmissionLog::getProblemIndex, k)
                    .or()
                    .like(SubmissionLog::getVerdict, k));
        }

        wrapper.orderByDesc(SubmissionLog::getSubmitTime);

        // 4. 执行物理分页查询
        Page<SubmissionLog> logPage = new Page<>(pageNum, pageSize);
        submissionLogMapper.selectPage(logPage, wrapper);

        // 5. 转换为 VO
        Page<SubmissionTimelineVO> voPage = new Page<>(pageNum, pageSize, logPage.getTotal());
        List<SubmissionTimelineVO> vos = logPage.getRecords().stream().map(log -> {
            SubmissionTimelineVO vo = new SubmissionTimelineVO();
            vo.setSubmissionId(log.getSubmissionId());
            vo.setContestId(log.getContestId());
            vo.setProblemIndex(log.getProblemIndex());
            vo.setProblemName(log.getProblemName());
            vo.setProblemUrl(log.getProblemUrl());
            vo.setVerdict(log.getVerdict());
            vo.setSubmitTime(log.getSubmitTime());
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(vos);
        return voPage;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefreshResultVO refreshLight(Long userId, String platformCode, Integer count) {
        // 1. 基础校验
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";
        Platform p = platformMapper.selectOne(new LambdaQueryWrapper<Platform>().eq(Platform::getCode, platformCode));
        if (p == null) throw new IllegalArgumentException("平台不存在");

        UserPlatformAccount account = upaMapper.selectOne(new LambdaQueryWrapper<UserPlatformAccount>()
                .eq(UserPlatformAccount::getUserId, userId)
                .eq(UserPlatformAccount::getPlatformId, p.getId()));
        if (account == null) throw new IllegalArgumentException("未绑定平台账号");
        String handle = account.getIdentifierValue();

        // 2. 同步 Rating
        try {
            List<CfUserRatingResponse.Item> ratings = cfClient.getUserRating(handle);
            RatingSnapshot last = ratingSnapshotMapper.selectOne(new LambdaQueryWrapper<RatingSnapshot>()
                    .eq(RatingSnapshot::getUserId, userId)
                    .eq(RatingSnapshot::getPlatformId, p.getId())
                    .eq(RatingSnapshot::getHandle, handle) // Fix: 增加 handle 过滤
                    .orderByDesc(RatingSnapshot::getSnapshotTime)
                    .last("LIMIT 1"));
            LocalDateTime lastTime = (last != null) ? last.getSnapshotTime() : null;

            for (CfUserRatingResponse.Item it : ratings) {
                LocalDateTime t = Instant.ofEpochSecond(it.getRatingUpdateTimeSeconds()).atZone(ZONE).toLocalDateTime();
                if (lastTime != null && !t.isAfter(lastTime)) continue;

                RatingSnapshot rs = new RatingSnapshot();
                rs.setUserId(userId);
                rs.setPlatformId(p.getId());
                rs.setHandle(handle);
                rs.setRating(it.getNewRating());
                rs.setContestName(it.getContestName());
                rs.setContestRank(it.getRank());
                rs.setSnapshotTime(t);
                ratingSnapshotMapper.insert(rs);
            }
        } catch (Exception e) {
            e.printStackTrace(); // 忽略网络错误，继续同步提交
        }

        // 3. 同步 Submissions
        int batchSize = 1000;
        int from = 1;
        int inserted = 0;
        int totalFetched = 0;
        Set<LocalDate> affectedDays = new HashSet<>();

        // 限制最大同步数量防止超时 (根据 count 参数)
        int maxCount = (count == null || count <= 0) ? 2000 : count;

        while (true) {
            List<CfUserStatusResponse.Submission> subs = cfClient.getUserStatus(handle, from, batchSize);
            if (subs == null || subs.isEmpty()) break;

            // 如果获取的数据超过了需要同步的范围，可以提前 break（根据 id 判断），这里简化为按数量
            totalFetched += subs.size();

            for (CfUserStatusResponse.Submission s : subs) {
                Long sec = s.getCreationTimeSeconds();
                if (sec == null) continue;
                LocalDateTime submitTime = Instant.ofEpochSecond(sec).atZone(ZONE).toLocalDateTime();
                affectedDays.add(submitTime.toLocalDate());

                Integer contestId = (s.getProblem() == null) ? null : s.getProblem().getContestId();
                String idx = (s.getProblem() == null) ? null : s.getProblem().getIndex();
                String name = (s.getProblem() == null) ? null : s.getProblem().getName();
                String url = (contestId != null && idx != null)
                        ? ("https://codeforces.com/contest/" + contestId + "/problem/" + idx) : null;

                int rows = submissionLogMapper.insertIgnore(userId, p.getId(), handle, s.getId(),
                        contestId, idx, name, url, s.getVerdict(), submitTime);
                if (rows > 0) inserted++;

                if ("OK".equalsIgnoreCase(s.getVerdict()) && contestId != null && idx != null) {
                    String key = contestId + "_" + idx;
                    solvedProblemMapper.insertIgnore(userId, p.getId(), handle,
                            contestId, idx, key, name, url, submitTime);
                }
            }

            if (totalFetched >= maxCount || subs.size() < batchSize) break;
            from += batchSize;
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        // 4. 重算热力图
        for (LocalDate day : affectedDays) {
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

            // 修正：增加 handle 参数
            int solved = solvedProblemMapper.countSolvedInRange(userId, p.getId(), handle, dayStart, dayEnd);

            Long submitCnt = submissionLogMapper.selectCount(new LambdaQueryWrapper<SubmissionLog>()
                    .eq(SubmissionLog::getUserId, userId)
                    .eq(SubmissionLog::getPlatformId, p.getId())
                    .eq(SubmissionLog::getHandle, handle) // Fix: 增加 handle 过滤
                    .ge(SubmissionLog::getSubmitTime, dayStart)
                    .lt(SubmissionLog::getSubmitTime, dayEnd));

            Long acceptCnt = submissionLogMapper.selectCount(new LambdaQueryWrapper<SubmissionLog>()
                    .eq(SubmissionLog::getUserId, userId)
                    .eq(SubmissionLog::getPlatformId, p.getId())
                    .eq(SubmissionLog::getHandle, handle) // Fix: 增加 handle 过滤
                    .eq(SubmissionLog::getVerdict, "OK")
                    .ge(SubmissionLog::getSubmitTime, dayStart)
                    .lt(SubmissionLog::getSubmitTime, dayEnd));

            dailyActivityMapper.upsert(userId, p.getId(), handle, day,
                    submitCnt.intValue(), acceptCnt.intValue(), solved);
        }

        RefreshResultVO vo = new RefreshResultVO();
        vo.setFetched(totalFetched);
        vo.setInserted(inserted);
        return vo;
    }
}