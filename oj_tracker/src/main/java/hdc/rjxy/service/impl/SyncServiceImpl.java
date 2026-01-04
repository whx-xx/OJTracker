package hdc.rjxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.cf.CfClientException;
import hdc.rjxy.cf.CfUserRatingResponse;
import hdc.rjxy.cf.CfUserStatusResponse;
import hdc.rjxy.cf.CodeforcesClient;
import hdc.rjxy.mapper.*;
import hdc.rjxy.pojo.*;
import hdc.rjxy.pojo.vo.*;
import hdc.rjxy.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {

    private final SyncJobLogMapper syncJobLogMapper;
    private final SyncUserLogMapper syncUserLogMapper;
    private final UserPlatformAccountMapper upaMapper;
    private final UserMapper userMapper;
    private final CodeforcesClient cfClient;
    private final RatingSnapshotMapper ratingSnapshotMapper;
    private final DailyActivityMapper dailyActivityMapper;
    private final SubmissionLogMapper submissionLogMapper;
    private final SolvedProblemMapper solvedProblemMapper;

    private static final Long PLATFORM_CF = 1L;
    private static final ZoneId ZONE_CN = ZoneId.of("Asia/Shanghai");

    // 解析详情字符串的正则: "newSub=10, newSolved=5"
    private static final Pattern DETAIL_PATTERN = Pattern.compile("newSub=(\\d+), newSolved=(\\d+)");

    // ================= 管理端查询接口 =================

    @Override
    public Page<SyncJobLogVO> pageJobs(int page, int pageSize, String jobType) {
        // 1. 查询 PO 分页
        Page<SyncJobLog> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<SyncJobLog> wrapper = new LambdaQueryWrapper<>();
        if (jobType != null && !jobType.isBlank()) {
            wrapper.eq(SyncJobLog::getJobType, jobType);
        }
        wrapper.orderByDesc(SyncJobLog::getStartTime);

        syncJobLogMapper.selectPage(p, wrapper);

        // 2. 转换 VO 分页
        Page<SyncJobLogVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        List<SyncJobLogVO> vos = p.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public SyncJobDetailVO jobDetail(Long jobId) {
        SyncJobLog job = syncJobLogMapper.selectById(jobId);
        if (job == null) throw new IllegalArgumentException("Job not found");

        // 1. 处理失败列表
        // 排除 SUCCESS 和 SKIP 状态
        List<SyncUserLog> failLogs = syncUserLogMapper.selectList(new LambdaQueryWrapper<SyncUserLog>()
                .eq(SyncUserLog::getJobId, jobId)
                .ne(SyncUserLog::getStatus, "SUCCESS")
                .ne(SyncUserLog::getStatus, "SKIP"));

        List<SyncUserFailVO> failVOs = new ArrayList<>();

        for (SyncUserLog log : failLogs) {
            SyncUserFailVO vo = new SyncUserFailVO();
            BeanUtils.copyProperties(log, vo);

            // 补充 Handle
            UserPlatformAccount upa = upaMapper.selectOne(new LambdaQueryWrapper<UserPlatformAccount>()
                    .eq(UserPlatformAccount::getUserId, log.getUserId())
                    .eq(UserPlatformAccount::getPlatformId, log.getPlatformId()));
            if (upa != null) {
                vo.setHandle(upa.getIdentifierValue());
            }
            vo.setErrorCodeDesc(log.getErrorCode());
            failVOs.add(vo);
        }

        // 2. 处理变更列表 (成功且有数据变化的记录)
        List<SyncUserChangeVO> changeList = new ArrayList<>();

        List<SyncUserLog> successLogs = syncUserLogMapper.selectList(new LambdaQueryWrapper<SyncUserLog>()
                .eq(SyncUserLog::getJobId, jobId)
                .eq(SyncUserLog::getStatus, "SUCCESS"));

        List<SyncUserChangeVO> tempChanges = new ArrayList<>();
        Set<Long> successUserIds = new HashSet<>();

        for (SyncUserLog l : successLogs) {
            // 注意：因为没有 details 字段，成功时的统计数据存储在 errorMessage 字段中
            String info = l.getErrorMessage();
            if (info == null || info.isEmpty()) continue;

            // 正则解析 "newSub=10, newSolved=5"
            Matcher m = DETAIL_PATTERN.matcher(info);
            if (m.find()) {
                try {
                    int newSub = Integer.parseInt(m.group(1));
                    int newSolved = Integer.parseInt(m.group(2));

                    // 只有当有新提交或新解决时才加入列表
                    if (newSub > 0 || newSolved > 0) {
                        SyncUserChangeVO vo = new SyncUserChangeVO();
                        vo.setUserId(l.getUserId());
                        vo.setNewSub(newSub);
                        vo.setNewSolved(newSolved);
                        vo.setRawDetails(info); // 这里使用 info (即 errorMessage)

                        tempChanges.add(vo);
                        successUserIds.add(l.getUserId());
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // 批量查询变更用户的 Handle
        Map<Long, String> handleMap = new HashMap<>();
        if (!successUserIds.isEmpty()) {
            List<UserPlatformAccount> upas = upaMapper.selectList(new LambdaQueryWrapper<UserPlatformAccount>()
                    .in(UserPlatformAccount::getUserId, successUserIds)
                    .eq(UserPlatformAccount::getPlatformId, PLATFORM_CF));
            for (UserPlatformAccount upa : upas) {
                handleMap.put(upa.getUserId(), upa.getIdentifierValue());
            }
        }

        // 填充变更列表 Handle
        for (SyncUserChangeVO vo : tempChanges) {
            vo.setHandle(handleMap.getOrDefault(vo.getUserId(), "ID:" + vo.getUserId()));
            changeList.add(vo);
        }

        // 3. 组装结果
        SyncJobDetailVO res = new SyncJobDetailVO();
        res.setJob(toVO(job));
        res.setFailList(failVOs);
        res.setChangeList(changeList);
        return res;
    }

    @Override
    public SyncOverviewVO overview(int recentLimit) {
        SyncOverviewVO vo = new SyncOverviewVO();

        // 最新 Rating
        SyncJobLog ratingJob = syncJobLogMapper.selectOne(new LambdaQueryWrapper<SyncJobLog>()
                .eq(SyncJobLog::getJobType, "RATING_SYNC")
                .orderByDesc(SyncJobLog::getStartTime)
                .last("LIMIT 1"));
        vo.setLatestRating(toVO(ratingJob));

        // 最新 Daily
        SyncJobLog dailyJob = syncJobLogMapper.selectOne(new LambdaQueryWrapper<SyncJobLog>()
                .eq(SyncJobLog::getJobType, "DAILY_SYNC")
                .orderByDesc(SyncJobLog::getStartTime)
                .last("LIMIT 1"));
        vo.setLatestDaily(toVO(dailyJob));

        // 最近记录
        Page<SyncJobLog> p = new Page<>(1, recentLimit);
        syncJobLogMapper.selectPage(p, new LambdaQueryWrapper<SyncJobLog>().orderByDesc(SyncJobLog::getStartTime));
        vo.setRecent(p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));

        return vo;
    }

    // ================= 核心调度逻辑 =================

    @Override
    public Long runCfRatingSync(String triggerSource) {
        LocalDateTime start = LocalDateTime.now();
        Long jobId = createJob("RATING_SYNC", triggerSource, start);

        List<UserPlatformAccount> accounts = listAllCfAccounts();

        int total = accounts.size();
        int success = 0, fail = 0, skip = 0;

        for (UserPlatformAccount account : accounts) {
            String res = processSingleUserRating(jobId, account.getUserId(), account.getIdentifierValue());
            if ("SUCCESS".equals(res)) success++;
            else if ("SKIP".equals(res)) skip++;
            else fail++;

            sleepRandom();
        }

        finishJob(jobId, start, total, success, fail, skip);
        return jobId;
    }

    @Override
    public Long runCfDailySync(String triggerSource, int days) {
        LocalDateTime start = LocalDateTime.now();
        Long jobId = createJob("DAILY_SYNC", triggerSource, start);

        List<UserPlatformAccount> accounts = listAllCfAccounts();
        int incDays = Math.max(2, Math.min(days, 7));

        int total = accounts.size();
        int success = 0, fail = 0, skip = 0;

        for (UserPlatformAccount account : accounts) {
            String res = processSingleUserDaily(jobId, account.getUserId(), account.getIdentifierValue(), incDays);
            if ("SUCCESS".equals(res)) success++;
            else if ("SKIP".equals(res)) skip++;
            else fail++;
            sleepRandom();
        }

        finishJob(jobId, start, total, success, fail, skip);
        return jobId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long rerunFailedUsers(Long oldJobId, String triggerSource) {
        SyncJobLog oldJob = syncJobLogMapper.selectById(oldJobId);
        if (oldJob == null) throw new IllegalArgumentException("Original job not found");

        String jobType = oldJob.getJobType();
        LocalDateTime start = LocalDateTime.now();
        Long newJobId = createJob(jobType, triggerSource, start);

        // 查出所有失败的用户ID
        List<SyncUserLog> fails = syncUserLogMapper.selectList(new LambdaQueryWrapper<SyncUserLog>()
                .eq(SyncUserLog::getJobId, oldJobId)
                .ne(SyncUserLog::getStatus, "SUCCESS")
                .ne(SyncUserLog::getStatus, "SKIP"));

        if (fails.isEmpty()) {
            finishJob(newJobId, start, 0, 0, 0, 0);
            return newJobId;
        }

        int total = fails.size();
        int success = 0, fail = 0, skip = 0;

        for (SyncUserLog failLog : fails) {
            Long userId = failLog.getUserId();
            UserPlatformAccount upa = upaMapper.selectOne(new LambdaQueryWrapper<UserPlatformAccount>()
                    .eq(UserPlatformAccount::getUserId, userId)
                    .eq(UserPlatformAccount::getPlatformId, PLATFORM_CF));

            String status = "SKIP";
            if (upa != null && upa.getIdentifierValue() != null) {
                String handle = upa.getIdentifierValue().trim();
                if ("RATING_SYNC".equals(jobType)) {
                    status = processSingleUserRating(newJobId, userId, handle);
                } else if ("DAILY_SYNC".equals(jobType)) {
                    status = processSingleUserDaily(newJobId, userId, handle, 3);
                }
            } else {
                logUserResult(newJobId, userId, "SKIP", "NO_HANDLE", "Handle missing during rerun");
            }

            if ("SUCCESS".equals(status)) success++;
            else if ("SKIP".equals(status)) skip++;
            else fail++;

            sleepRandom();
        }

        finishJob(newJobId, start, total, success, fail, skip);
        return newJobId;
    }

    // ================= 单个用户处理逻辑 =================

    private String processSingleUserRating(Long jobId, Long userId, String handle) {
        try {
            RatingSnapshot lastSnapshot = ratingSnapshotMapper.selectOne(new LambdaQueryWrapper<RatingSnapshot>()
                    .eq(RatingSnapshot::getUserId, userId)
                    .eq(RatingSnapshot::getPlatformId, PLATFORM_CF)
                    .orderByDesc(RatingSnapshot::getSnapshotTime)
                    .last("LIMIT 1"));

            LocalDateTime lastTime = (lastSnapshot == null) ? null : lastSnapshot.getSnapshotTime();
            List<CfUserRatingResponse.Item> items = cfClient.getUserRating(handle);

            int inserted = 0;
            for (CfUserRatingResponse.Item it : items) {
                LocalDateTime t = Instant.ofEpochSecond(it.getRatingUpdateTimeSeconds())
                        .atZone(ZONE_CN).toLocalDateTime();
                if (lastTime != null && !t.isAfter(lastTime)) continue;

                RatingSnapshot snapshot = new RatingSnapshot();
                snapshot.setUserId(userId);
                snapshot.setPlatformId(PLATFORM_CF);
                snapshot.setHandle(handle);
                snapshot.setContestName(it.getContestName());
                snapshot.setContestRank(it.getRank());
                snapshot.setSnapshotTime(t);
                snapshot.setRating(it.getNewRating());
                ratingSnapshotMapper.insert(snapshot);
                inserted++;
            }

            if (inserted == 0) {
                logUserResult(jobId, userId, "SKIP", "RATING_UNCHANGED", "无新增比赛");
                return "SKIP";
            } else {
                // 成功时将统计数据写入 errorMessage 字段
                logUserResult(jobId, userId, "SUCCESS", null, "newSub=0, newSolved=0, ratingChanges=" + inserted);
                return "SUCCESS";
            }
        } catch (Exception e) {
            log.error("Rating sync fail user={}", userId, e);
            logUserResult(jobId, userId, "FAIL", "UNKNOWN", e.getMessage());
            return "FAIL";
        }
    }

    private String processSingleUserDaily(Long jobId, Long userId, String handle, int incDays) {
        LocalDate today = LocalDate.now(ZONE_CN);
        try {
            DailyActivity lastActivity = dailyActivityMapper.selectOne(new LambdaQueryWrapper<DailyActivity>()
                    .eq(DailyActivity::getUserId, userId)
                    .eq(DailyActivity::getPlatformId, PLATFORM_CF)
                    .orderByDesc(DailyActivity::getDay)
                    .last("LIMIT 1"));

            boolean firstSync = (lastActivity == null);
            Long lastMaxId = null;
            SubmissionLog maxLog = submissionLogMapper.selectOne(new LambdaQueryWrapper<SubmissionLog>()
                    .select(SubmissionLog::getSubmissionId)
                    .eq(SubmissionLog::getUserId, userId)
                    .eq(SubmissionLog::getPlatformId, PLATFORM_CF)
                    .orderByDesc(SubmissionLog::getSubmissionId)
                    .last("LIMIT 1"));
            if (maxLog != null) lastMaxId = maxLog.getSubmissionId();

            boolean needFull = (lastMaxId == null);
            int from = 1;
            int count = needFull ? 2000 : 10;
            int newSubmissions = 0, newSolved = 0;
            LocalDate firstSubmitDateInThisBatch = null;

            while (true) {
                List<CfUserStatusResponse.Submission> subs = cfClient.getUserStatus(handle, from, count);

                if (subs == null || subs.isEmpty()) break;
                if (from > 1000000) {
                    log.warn("User {} submissions exceed limit 1,000,000, stop syncing.", userId);
                    break;
                }

                boolean shouldStop = false;
                for (CfUserStatusResponse.Submission s : subs) {
                    if (s.getId() == null || s.getCreationTimeSeconds() == null) continue;

                    if (!needFull && s.getId() <= lastMaxId) {
                        shouldStop = true;
                        break;
                    }

                    LocalDateTime submitTime = Instant.ofEpochSecond(s.getCreationTimeSeconds()).atZone(ZONE_CN).toLocalDateTime();
                    LocalDate day = submitTime.toLocalDate();

                    if (firstSubmitDateInThisBatch == null || day.isBefore(firstSubmitDateInThisBatch)) {
                        firstSubmitDateInThisBatch = day;
                    }

                    Integer rating = (s.getProblem() != null) ? s.getProblem().getRating() : null;
                    Integer contestId = (s.getProblem() == null) ? null : s.getProblem().getContestId();
                    String idx = (s.getProblem() == null) ? null : s.getProblem().getIndex();
                    String name = (s.getProblem() == null) ? null : s.getProblem().getName();
                    String url = (contestId != null && idx != null)
                            ? ("https://codeforces.com/contest/" + contestId + "/problem/" + idx) : null;

                    int ins = submissionLogMapper.insertIgnore(userId, PLATFORM_CF, handle, s.getId(),
                            contestId, idx, name, url, s.getVerdict(), rating, submitTime);
                    if (ins > 0) newSubmissions++;

                    if ("OK".equalsIgnoreCase(s.getVerdict()) && s.getProblem() != null && s.getProblem().getContestId() != null) {
                        String key = s.getProblem().getContestId() + "_" + s.getProblem().getIndex();
                        int insSol = solvedProblemMapper.insertIgnore(userId, PLATFORM_CF, handle,
                                contestId, idx, key, name, url, rating, submitTime);
                        if (insSol > 0) newSolved++;
                    }
                }

                if (shouldStop) break;
                from += count;
                if (count < 2000) count = 2000;
            }

            LocalDate minDayForDaily = null;
            if (firstSync) {
                minDayForDaily = firstSubmitDateInThisBatch;
            } else {
                minDayForDaily = today.minusDays(incDays - 1L);
                if (newSubmissions > 0 && lastActivity != null) {
                    LocalDate lastDay = lastActivity.getDay();
                    if (lastDay.minusDays(1).isBefore(minDayForDaily)) {
                        minDayForDaily = lastDay.minusDays(1);
                    }
                }
            }

            if (minDayForDaily != null) {
                if (minDayForDaily.isAfter(today)) minDayForDaily = today;
                for (LocalDate d = minDayForDaily; !d.isAfter(today); d = d.plusDays(1)) {
                    LocalDateTime dayStart = d.atStartOfDay();
                    LocalDateTime dayEnd = d.plusDays(1).atStartOfDay().minusSeconds(1);

                    int realSubmitCnt = submissionLogMapper.countByDate(userId, PLATFORM_CF, dayStart, dayEnd);
                    int realAcceptCnt = submissionLogMapper.countAcceptByDate(userId, PLATFORM_CF, dayStart, dayEnd);
                    int realSolvedCnt = submissionLogMapper.countDistinctSolvedByDate(userId, PLATFORM_CF, dayStart, dayEnd);

                    dailyActivityMapper.upsert(userId, PLATFORM_CF, handle, d,
                            realSubmitCnt, realAcceptCnt, realSolvedCnt);
                }
            }

            // 成功时将统计数据写入 errorMessage 字段
            logUserResult(jobId, userId, "SUCCESS", null, "newSub=" + newSubmissions + ", newSolved=" + newSolved);
            return "SUCCESS";

        } catch (Exception e) {
            log.error("Daily sync fail user={}", userId, e);
            logUserResult(jobId, userId, "FAIL", "UNKNOWN", e.getMessage());
            return "FAIL";
        }
    }

    // ================= 辅助方法 =================

    private List<UserPlatformAccount> listAllCfAccounts() {
        return upaMapper.selectList(new LambdaQueryWrapper<UserPlatformAccount>()
                .eq(UserPlatformAccount::getPlatformId, PLATFORM_CF)
                .isNotNull(UserPlatformAccount::getIdentifierValue)
                .ne(UserPlatformAccount::getIdentifierValue, ""));
    }

    private Long createJob(String jobType, String triggerSource, LocalDateTime start) {
        SyncJobLog job = new SyncJobLog();
        job.setJobType(jobType);
        job.setStatus("RUNNING");
        job.setStartTime(start);
        job.setTriggerSource(triggerSource != null ? triggerSource : "MANUAL");
        job.setCreatedAt(LocalDateTime.now());
        syncJobLogMapper.insert(job);
        return job.getId();
    }

    private void finishJob(Long jobId, LocalDateTime start, int total, int success, int fail, int skip) {
        LocalDateTime end = LocalDateTime.now();
        SyncJobLog job = new SyncJobLog();
        job.setId(jobId);
        job.setEndTime(end);
        job.setDurationMs(Duration.between(start, end).toMillis());
        job.setTotalCount(total);
        job.setSuccessCount(success);
        job.setFailCount(fail);
        job.setMessage(String.format("S=%d, F=%d, K=%d", success, fail, skip));
        job.setStatus(fail == 0 ? "SUCCESS" : (success == 0 ? "FAIL" : "PARTIAL_FAIL"));
        syncJobLogMapper.updateById(job);
    }

    private void logUserResult(Long jobId, Long userId, String status, String errCode, String msg) {
        SyncUserLog uLog = new SyncUserLog();
        uLog.setJobId(jobId);
        uLog.setUserId(userId);
        uLog.setPlatformId(PLATFORM_CF);
        uLog.setStatus(status);
        uLog.setErrorCode(errCode);

        // 无论成功还是失败，都将 msg 写入 errorMessage 字段
        // 成功时 msg 包含 newSub=X, newSolved=Y
        // 失败时 msg 包含 异常堆栈或错误描述
        uLog.setErrorMessage(msg != null && msg.length() > 500 ? msg.substring(0, 500) : msg);

        uLog.setFetchedAt(LocalDateTime.now());
        syncUserLogMapper.insert(uLog);
    }

    private SyncJobLogVO toVO(SyncJobLog log) {
        if (log == null) return null;
        SyncJobLogVO vo = new SyncJobLogVO();
        vo.setId(log.getId());
        vo.setJobType(log.getJobType());
        vo.setStatus(log.getStatus());
        vo.setStartTime(log.getStartTime());
        vo.setEndTime(log.getEndTime());
        vo.setDurationMs(log.getDurationMs());
        vo.setSuccessCount(log.getSuccessCount());
        vo.setFailCount(log.getFailCount());
        vo.setTriggerSource(log.getTriggerSource());
        return vo;
    }

    private void sleepRandom() {
        try { Thread.sleep(300 + new Random().nextInt(500)); } catch (InterruptedException e) {}
    }
}