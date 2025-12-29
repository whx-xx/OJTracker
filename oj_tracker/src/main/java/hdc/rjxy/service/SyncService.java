package hdc.rjxy.service;

import hdc.rjxy.cf.CfUserRatingResponse;
import hdc.rjxy.cf.CfUserStatusResponse;
import hdc.rjxy.common.SyncErrorCodeDict;
import hdc.rjxy.mapper.*;
import hdc.rjxy.pojo.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import hdc.rjxy.cf.CodeforcesClient;
import hdc.rjxy.cf.CfClientException;


@Service
public class SyncService {

    private final SyncLogMapper syncLogMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final UserPlatformAccountMapper upaMapper;
    private final CodeforcesClient cfClient;
    private final RatingSnapshotMapper ratingSnapshotMapper;
    private final DailyActivityMapper dailyActivityMapper;
    private final SubmissionLogMapper submissionLogMapper;
    private final SolvedProblemMapper solvedProblemMapper;


    public SyncService(SyncLogMapper syncLogMapper,
                       TeamMemberMapper teamMemberMapper,
                       UserPlatformAccountMapper upaMapper,
                       CodeforcesClient cfClient,
                       RatingSnapshotMapper ratingSnapshotMapper,
                       DailyActivityMapper dailyActivityMapper,
                       SubmissionLogMapper submissionLogMapper,
                       SolvedProblemMapper solvedProblemMapper) {
        this.syncLogMapper = syncLogMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.upaMapper = upaMapper;
        this.cfClient = cfClient;
        this.ratingSnapshotMapper = ratingSnapshotMapper;
        this.dailyActivityMapper = dailyActivityMapper;
        this.submissionLogMapper = submissionLogMapper;
        this.solvedProblemMapper = solvedProblemMapper;
    }

    public List<SyncJobLogVO> pageJobs(int page, int pageSize, String jobType) {
        int offset = (page - 1) * pageSize;
        return syncLogMapper.pageJobs(offset, pageSize, jobType);
    }

    public long countJobs(String jobType) {
        return syncLogMapper.countJobs(jobType);
    }

    public SyncJobDetailVO jobDetail(Long jobId) {
        SyncJobLogVO job = syncLogMapper.findJob(jobId);
        if (job == null) throw new IllegalArgumentException("job不存在");

        List<SyncUserFailVO> failList = syncLogMapper.listFailUsers(jobId);
        for (SyncUserFailVO f : failList) {
            SyncErrorCodeDict.Info info = SyncErrorCodeDict.get(f.getErrorCode());
            f.setErrorCodeDesc(info.desc);
            f.setSuggestAction(info.action);
            f.setRetryable(info.retryable);
        }

        SyncJobDetailVO vo = new SyncJobDetailVO();
        vo.setJob(job);
        vo.setFailList(failList);
        return vo;
    }


    @Transactional
    public Long runCfRatingSync(String triggerSource) {

        final String jobType = "RATING_SYNC";
        final Long platformId = 1L; // CF
        final ZoneId zone = ZoneId.of("Asia/Shanghai");

        if (triggerSource == null || triggerSource.isBlank()) {
            triggerSource = "MANUAL";
        }

        LocalDateTime start = LocalDateTime.now();
        syncLogMapper.insertJob(jobType, "RUNNING", start, triggerSource);;
        Long jobId = syncLogMapper.lastInsertId();

        List<TeamMemberSimpleVO> members =
                teamMemberMapper.listEnabledMembers("DEFAULT");

        int total = members.size();
        int success = 0;
        int fail = 0;
        int skip = 0;

        for (TeamMemberSimpleVO m : members) {
            Long userId = m.getUserId();
            String handle = upaMapper.findIdentifierValue(userId, platformId);

            if (handle == null || handle.isBlank()) {
                syncLogMapper.insertUserLog(jobId, userId, platformId,
                        "SKIP", "HANDLE_MISSING", "未绑定 Codeforces");
                skip++;
                continue;
            }

            try {
                RatingPointVO last =
                        ratingSnapshotMapper.findLast(userId, platformId, handle.trim());
                LocalDateTime lastTime =
                        (last == null) ? null : last.getTime();

                List<CfUserRatingResponse.Item> items =
                        cfClient.getUserRating(handle.trim());

                int inserted = 0;

                for (CfUserRatingResponse.Item it : items) {
                    LocalDateTime t = Instant
                            .ofEpochSecond(it.getRatingUpdateTimeSeconds())
                            .atZone(zone)
                            .toLocalDateTime();

                    if (lastTime != null && !t.isAfter(lastTime)) {
                        continue;
                    }

                    // [修复] 传入 rank 字段
                    ratingSnapshotMapper.insert(userId, platformId, handle.trim(),
                            it.getNewRating(), it.getContestName(), it.getRank(), t);

                    inserted++;
                }

                if (inserted == 0) {
                    syncLogMapper.insertUserLog(jobId, userId, platformId,
                            "SKIP", "RATING_UNCHANGED", "无新增比赛");
                    skip++;
                } else {
                    syncLogMapper.insertUserLog(jobId, userId, platformId,
                            "SUCCESS", null, "新增 " + inserted + " 条");
                    success++;
                }

            } catch (CfClientException e) {
                // CF 已分类错误
                syncLogMapper.insertUserLog(
                        jobId,
                        userId,
                        platformId,
                        "FAIL",
                        e.getCode(),          // 用 CF_CLIENT 给你的 code
                        e.getMessage()
                );
                fail++;

            } catch (Exception e) {
                // 非 CF 错误（DB、空指针、你自己代码 bug）
                syncLogMapper.insertUserLog(
                        jobId,
                        userId,
                        platformId,
                        "FAIL",
                        "UNKNOWN",
                        e.getMessage()
                );
                fail++;
            }

        }

        LocalDateTime end = LocalDateTime.now();
        long durationMs = java.time.Duration.between(start, end).toMillis();

        String status;
        if (fail == 0) status = "SUCCESS";
        else if (success == 0) status = "FAIL";
        else status = "PARTIAL_FAIL";

        String message = "SUCCESS=" + success +
                ", FAIL=" + fail +
                ", SKIP=" + skip;

        syncLogMapper.updateJobFinish(
                jobId, status, end, durationMs,
                total, success, fail, message
        );

        return jobId;
    }


    @Transactional
    public Long runCfDailySync(String triggerSource, int days) {
        // ... (保持不变) ...
        final String jobType = "DAILY_SYNC";
        final Long platformId = 1L; // CF
        final String teamCode = "DEFAULT";
        final ZoneId zone = ZoneId.of("Asia/Shanghai");

        // 后续增量：只补最近 1~2 天（有定时任务）
        int incDays = Math.max(1, Math.min(days, 2));

        if (triggerSource == null || triggerSource.isBlank()) {
            triggerSource = "MANUAL";
        }

        LocalDateTime start = LocalDateTime.now();
        syncLogMapper.insertJob(jobType, "RUNNING", start, triggerSource);
        Long jobId = syncLogMapper.lastInsertId();

        List<TeamMemberSimpleVO> members = teamMemberMapper.listEnabledMembers(teamCode);

        int total = members.size();
        int success = 0;
        int fail = 0;
        int skip = 0;

        LocalDate today = LocalDate.now(zone);

        for (TeamMemberSimpleVO m : members) {
            Long userId = m.getUserId();

            String handle = upaMapper.findIdentifierValue(userId, platformId);
            if (handle == null || handle.isBlank()) {
                syncLogMapper.insertUserLog(jobId, userId, platformId,
                        "SKIP", "HANDLE_MISSING", "未绑定 Codeforces handle");
                skip++;
                sleepRandom();
                continue;
            }
            handle = handle.trim();

            try {
                // 是否首次：daily_activity 没记录
                LocalDate lastDay = dailyActivityMapper.findLastDay(userId, platformId, handle);
                boolean firstSync = (lastDay == null);

                // daily_activity 的写入窗口
                LocalDate minDayForDaily;
                if (firstSync) {
                    minDayForDaily = null; // 首次全量：后面从 submissions 算出最早 day，但只写“有提交的天”
                } else {
                    minDayForDaily = today.minusDays(incDays - 1L);
                    LocalDate overlap = lastDay.minusDays(1);
                    if (overlap.isBefore(minDayForDaily)) minDayForDaily = overlap;
                }

                // 聚合容器（按天）
                Map<LocalDate, Integer> submitCnt = new HashMap<>();
                Map<LocalDate, Integer> acceptCnt = new HashMap<>();
                Map<LocalDate, Set<String>> solvedSet = new HashMap<>();

                // 增量判断：submission_id
                Long lastMaxId = submissionLogMapper.findMaxSubmissionId(userId, platformId, handle);
                boolean needFull = (lastMaxId == null);

                final int pageSize = 1000;
                final int maxPages = 200; // 安全阀
                int from = 1;
                int pages = 0;

                int newSubmissions = 0;
                int newSolved = 0;

                while (pages < maxPages) {
                    List<CfUserStatusResponse.Submission> subs = cfClient.getUserStatus(handle, from, pageSize);
                    if (subs == null || subs.isEmpty()) break;

                    boolean shouldStop = false;

                    for (CfUserStatusResponse.Submission s : subs) {
                        Long submissionId = s.getId();
                        Long sec = s.getCreationTimeSeconds();
                        if (sec == null) continue;

                        // 后续增量：遇到已同步过的 id -> 停止（因为更老）
                        if (!needFull && submissionId != null && submissionId <= lastMaxId) {
                            shouldStop = true;
                            break;
                        }

                        LocalDateTime submitTime = Instant.ofEpochSecond(sec).atZone(zone).toLocalDateTime();
                        LocalDate day = submitTime.toLocalDate();

                        // 题目字段
                        Integer contestId = (s.getProblem() == null) ? null : s.getProblem().getContestId();
                        String index = (s.getProblem() == null) ? null : s.getProblem().getIndex();
                        String name = (s.getProblem() == null) ? null : s.getProblem().getName();
                        String verdict = s.getVerdict();

                        String problemUrl = null;
                        if (contestId != null && index != null) {
                            problemUrl = "https://codeforces.com/problemset/problem/" + contestId + "/" + index;
                        }

                        // 写 submission_log（IGNORE 防重复）
                        if (submissionId != null) {
                            int ins = submissionLogMapper.insertIgnore(
                                    userId, platformId, handle,
                                    submissionId, contestId, index, name, problemUrl,
                                    verdict, submitTime
                            );
                            if (ins > 0) newSubmissions++;
                        }

                        // solved_problem：只记录第一次 AC（IGNORE）
                        if ("OK".equalsIgnoreCase(verdict) && contestId != null && index != null) {
                            String key = contestId + "_" + index;
                            int insSolved = solvedProblemMapper.insertIgnore(
                                    userId, platformId, handle,
                                    contestId, index, key, name, problemUrl,
                                    submitTime
                            );
                            if (insSolved > 0) newSolved++;
                        }

                        // daily_activity 聚合
                        // 首次：全量聚合所有 day；增量：只聚合 minDayForDaily..today（窗口外的不算）
                        if (!firstSync) {
                            if (day.isBefore(minDayForDaily) || day.isAfter(today)) continue;
                        } else {
                            if (minDayForDaily == null || day.isBefore(minDayForDaily)) {
                                minDayForDaily = day;
                            }
                        }

                        submitCnt.put(day, submitCnt.getOrDefault(day, 0) + 1);

                        if ("OK".equalsIgnoreCase(verdict)) {
                            acceptCnt.put(day, acceptCnt.getOrDefault(day, 0) + 1);
                            if (contestId != null && index != null) {
                                String k = contestId + "_" + index;
                                solvedSet.computeIfAbsent(day, d -> new HashSet<>()).add(k);
                            }
                        }
                    }

                    if (shouldStop) break;

                    from += pageSize;
                    pages++;
                    sleepRandom();
                }

                // 写 daily_activity
                if (firstSync) {
                    if (minDayForDaily == null) {
                        syncLogMapper.insertUserLog(jobId, userId, platformId,
                                "SUCCESS", null, "首次全量：无提交记录");
                        success++;
                    } else {
                        // 首次全量：只写“有提交的天”
                        for (Map.Entry<LocalDate, Integer> e : submitCnt.entrySet()) {
                            LocalDate d = e.getKey();
                            int sc = e.getValue();
                            int ac = acceptCnt.getOrDefault(d, 0);
                            int solved = solvedSet.getOrDefault(d, Collections.emptySet()).size();
                            dailyActivityMapper.upsert(userId, platformId, handle, d, sc, ac, solved);
                        }

                        syncLogMapper.insertUserLog(jobId, userId, platformId,
                                "SUCCESS", null, "首次全量：days=" + submitCnt.size()
                                        + ", newSub=" + newSubmissions
                                        + ", newSolved=" + newSolved);
                        success++;
                    }
                } else {
                    // 增量：范围很小，写 minDayForDaily..today（没提交也写 0）
                    for (LocalDate d = minDayForDaily; !d.isAfter(today); d = d.plusDays(1)) {
                        int sc = submitCnt.getOrDefault(d, 0);
                        int ac = acceptCnt.getOrDefault(d, 0);
                        int solved = solvedSet.getOrDefault(d, Collections.emptySet()).size();
                        dailyActivityMapper.upsert(userId, platformId, handle, d, sc, ac, solved);
                    }

                    syncLogMapper.insertUserLog(jobId, userId, platformId,
                            "SUCCESS", null, "增量：" + minDayForDaily + " ~ " + today
                                    + ", newSub=" + newSubmissions
                                    + ", newSolved=" + newSolved);
                    success++;
                }

            } catch (CfClientException e) {
                syncLogMapper.insertUserLog(jobId, userId, platformId,
                        "FAIL", e.getCode(), e.getMessage());
                fail++;
            } catch (Exception e) {
                syncLogMapper.insertUserLog(jobId, userId, platformId,
                        "FAIL", "UNKNOWN", e.getMessage());
                fail++;
            } finally {
                sleepRandom();
            }
        }

        LocalDateTime end = LocalDateTime.now();
        long durationMs = java.time.Duration.between(start, end).toMillis();

        String status;
        if (fail == 0) status = "SUCCESS";
        else if (success == 0) status = "FAIL";
        else status = "PARTIAL_FAIL";

        String message = "SUCCESS=" + success + ", FAIL=" + fail + ", SKIP=" + skip;

        syncLogMapper.updateJobFinish(jobId, status, end, durationMs,
                total, success, fail, message);

        return jobId;
    }


    private void sleepRandom() {
        try {
            long sleepMs = 300 + new java.util.Random().nextInt(501); // 300~800
            Thread.sleep(sleepMs);
        } catch (InterruptedException ignored) {
        }
    }


    public SyncOverviewVO overview(int recentLimit) {
        int lim = Math.max(1, Math.min(recentLimit, 50));

        SyncOverviewVO vo = new SyncOverviewVO();
        vo.setLatestRating(syncLogMapper.findLatestByType("RATING_SYNC"));
        vo.setLatestDaily(syncLogMapper.findLatestByType("DAILY_SYNC"));
        vo.setRecent(syncLogMapper.listRecent(lim, null));
        return vo;
    }


    @Transactional
    public Long rerunFailedUsers(Long oldJobId, String triggerSource) {
        // ... (保持不变) ...
        if (oldJobId == null) throw new IllegalArgumentException("jobId不能为空");

        SyncJobLogVO oldJob = syncLogMapper.findJob(oldJobId);
        if (oldJob == null) throw new IllegalArgumentException("原job不存在");

        String jobType = oldJob.getJobType();
        if (jobType == null || jobType.isBlank()) throw new IllegalArgumentException("原jobType为空");

        if (triggerSource == null || triggerSource.isBlank()) triggerSource = "MANUAL_RERUN";

        LocalDateTime start = LocalDateTime.now();
        syncLogMapper.insertJob(jobType, "RUNNING", start, triggerSource);
        Long newJobId = syncLogMapper.lastInsertId();

        List<SyncUserFailVO> fails = syncLogMapper.listFailUsers(oldJobId);
        if (fails == null || fails.isEmpty()) {
            LocalDateTime end = LocalDateTime.now();
            long durationMs = java.time.Duration.between(start, end).toMillis();
            syncLogMapper.updateJobFinish(newJobId, "SUCCESS", end, durationMs,
                    0, 0, 0, "SUCCESS=0, FAIL=0, SKIP=0");
            return newJobId;
        }

        int total = fails.size();
        int success = 0;
        int fail = 0;
        int skip = 0;

        final Long platformId = 1L;

        for (SyncUserFailVO u : fails) {
            Long userId = u.getUserId();
            try {
                if ("RATING_SYNC".equalsIgnoreCase(jobType)) {
                    rerunRatingForUser(newJobId, userId, platformId);
                    success++;
                } else if ("DAILY_SYNC".equalsIgnoreCase(jobType)) {
                    rerunDailyForUser(newJobId, userId, platformId, 2);
                    success++;
                } else {
                    syncLogMapper.insertUserLog(newJobId, userId, platformId,
                            "SKIP", "UNKNOWN_JOBTYPE", "不支持的jobType: " + jobType);
                    skip++;
                }
            } catch (CfClientException e) {
                syncLogMapper.insertUserLog(newJobId, userId, platformId,
                        "FAIL", e.getCode(), e.getMessage());
                fail++;
            } catch (Exception e) {
                syncLogMapper.insertUserLog(newJobId, userId, platformId,
                        "FAIL", "UNKNOWN", e.getMessage());
                fail++;
            } finally {
                sleepRandom();
            }
        }

        LocalDateTime end = LocalDateTime.now();
        long durationMs = java.time.Duration.between(start, end).toMillis();

        String status;
        if (fail == 0) status = "SUCCESS";
        else if (success == 0) status = "FAIL";
        else status = "PARTIAL_FAIL";

        String message = "SUCCESS=" + success + ", FAIL=" + fail + ", SKIP=" + skip;

        syncLogMapper.updateJobFinish(newJobId, status, end, durationMs,
                total, success, fail, message);

        return newJobId;
    }

    /** 只重跑某个用户的 rating */
    private void rerunRatingForUser(Long jobId, Long userId, Long platformId) {
        final ZoneId zone = ZoneId.of("Asia/Shanghai");

        String handle = upaMapper.findIdentifierValue(userId, platformId);
        if (handle == null || handle.isBlank()) {
            syncLogMapper.insertUserLog(jobId, userId, platformId,
                    "SKIP", "HANDLE_MISSING", "未绑定 Codeforces");
            return;
        }
        handle = handle.trim();

        RatingPointVO last = ratingSnapshotMapper.findLast(userId, platformId, handle);
        LocalDateTime lastTime = (last == null) ? null : last.getTime();

        List<CfUserRatingResponse.Item> items = cfClient.getUserRating(handle);

        int inserted = 0;
        for (CfUserRatingResponse.Item it : items) {
            LocalDateTime t = Instant.ofEpochSecond(it.getRatingUpdateTimeSeconds())
                    .atZone(zone).toLocalDateTime();

            if (lastTime != null && !t.isAfter(lastTime)) continue;

            // [修复] 传入 rank
            ratingSnapshotMapper.insert(userId, platformId, handle,
                    it.getNewRating(), it.getContestName(), it.getRank(), t);
            inserted++;
        }

        if (inserted == 0) {
            syncLogMapper.insertUserLog(jobId, userId, platformId,
                    "SKIP", "RATING_UNCHANGED", "无新增比赛");
        } else {
            syncLogMapper.insertUserLog(jobId, userId, platformId,
                    "SUCCESS", null, "新增 " + inserted + " 条");
        }
    }

    /** 只重跑某个用户的 daily（补最近 incDays 天） */
    private void rerunDailyForUser(Long jobId, Long userId, Long platformId, int incDays) {
        final ZoneId zone = ZoneId.of("Asia/Shanghai");

        String handle = upaMapper.findIdentifierValue(userId, platformId);
        if (handle == null || handle.isBlank()) {
            syncLogMapper.insertUserLog(jobId, userId, platformId,
                    "SKIP", "HANDLE_MISSING", "未绑定 Codeforces handle");
            return;
        }
        handle = handle.trim();

        incDays = Math.max(1, Math.min(incDays, 2));
        LocalDate today = LocalDate.now(zone);
        LocalDate minDay = today.minusDays(incDays - 1L);

        List<CfUserStatusResponse.Submission> subs = cfClient.getUserStatus(handle, 1, 2000);

        Map<LocalDate, Integer> submitCnt = new HashMap<>();
        Map<LocalDate, Integer> acceptCnt = new HashMap<>();
        Map<LocalDate, Set<String>> solvedSet = new HashMap<>();

        for (CfUserStatusResponse.Submission s : subs) {
            Long sec = s.getCreationTimeSeconds();
            if (sec == null) continue;

            LocalDate day = Instant.ofEpochSecond(sec).atZone(zone).toLocalDate();
            if (day.isBefore(minDay)) break;

            submitCnt.put(day, submitCnt.getOrDefault(day, 0) + 1);

            if ("OK".equalsIgnoreCase(s.getVerdict())) {
                acceptCnt.put(day, acceptCnt.getOrDefault(day, 0) + 1);

                String key = null;
                if (s.getProblem() != null
                        && s.getProblem().getContestId() != null
                        && s.getProblem().getIndex() != null) {
                    key = s.getProblem().getContestId() + "_" + s.getProblem().getIndex();
                }
                if (key != null) solvedSet.computeIfAbsent(day, k -> new HashSet<>()).add(key);
            }
        }

        for (LocalDate d = minDay; !d.isAfter(today); d = d.plusDays(1)) {
            int sc = submitCnt.getOrDefault(d, 0);
            int ac = acceptCnt.getOrDefault(d, 0);
            int solved = solvedSet.getOrDefault(d, Collections.emptySet()).size();

            dailyActivityMapper.upsert(userId, platformId, handle, d, sc, ac, solved);
        }

        syncLogMapper.insertUserLog(jobId, userId, platformId,
                "SUCCESS", null, "补 " + minDay + " ~ " + today);
    }

}