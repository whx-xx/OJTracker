package hdc.rjxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.cf.CfClientException;
import hdc.rjxy.cf.CfUserRatingResponse;
import hdc.rjxy.cf.CfUserStatusResponse;
import hdc.rjxy.cf.CodeforcesClient;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
// @RequiredArgsConstructor 注解会自动为类中
// 所有被 final 修饰的字段（以及被 @NonNull 标记的字段）生成一个包含这些参数的构造函数。
/*
相比字段注入（@Autowired on fields）的优势
这种写法（private final + @RequiredArgsConstructor）主要有以下几个核心优势：
1. 保证不可变性 (Immutability)：
依赖字段被声明为 final，这意味着它们在初始化后不能被修改。这有助于保证组件的状态安全，符合多线程编程的最佳实践。
字段注入（Field Injection）通常要求字段不能是 final，或者是通过反射强制赋值，这破坏了不可变性。
2. 空指针安全 (Null Safety)：
由于字段是 final 的，Java 编译器会强制要求在构造函数中必须对其赋值。这意味着当 SyncServiceImpl 被实例化时，所有的依赖都必须就位，避免了出现 Bean 部分初始化（Partially initialized）导致在使用时报 NullPointerException 的风险。
更易于单元测试 (Testability)：
在编写单元测试时（不启动 Spring 容器），你可以直接通过 new SyncServiceImpl(mockMapper1, mockMapper2, ...) 的方式手动实例化对象并注入 Mock 对象。
如果是字段注入，你往往需要使用反射工具或 @InjectMocks 等框架魔法才能把 Mock 对象塞进去，测试代码会变得复杂且难以脱离框架运行。
3. 避免循环依赖 (Circular Dependencies)：
构造器注入无法解决循环依赖问题（A 依赖 B，B 依赖 A）。如果存在循环依赖，Spring 会在应用启动时直接抛出异常，迫使开发者在设计阶段就发现并解决这个问题（通常意味着代码结构需要重构）。
字段注入可能会掩盖循环依赖，直到运行时才可能暴露问题。
4. 代码简洁：
不需要在每个字段上都写一行 @Autowired。
不需要手动编写冗长的构造函数代码，Lombok 自动完成了。
*/
public class SyncServiceImpl implements SyncService {

    private final SyncJobLogMapper syncJobLogMapper;
    private final SyncUserLogMapper syncUserLogMapper;
    private final UserPlatformAccountMapper upaMapper;
    private final UserMapper userMapper; // 用于关联查询用户名/handle
    private final CodeforcesClient cfClient;
    private final RatingSnapshotMapper ratingSnapshotMapper;
    private final DailyActivityMapper dailyActivityMapper;
    private final SubmissionLogMapper submissionLogMapper;
    private final SolvedProblemMapper solvedProblemMapper;

    private static final Long PLATFORM_CF = 1L;
    private static final ZoneId ZONE_CN = ZoneId.of("Asia/Shanghai");

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
        // 创建一个新的 Page 对象用于返回 VO，复制分页元数据（total, pages等）
        Page<SyncJobLogVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());

        // 转换记录列表
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

        // 查询失败的用户日志
        // 这里需要关联 user_platform_account 获取 handle，或者关联 user 表
        // 为简单起见，我们先查 Log，再手动填 Handle
        List<SyncUserLog> failLogs = syncUserLogMapper.selectList(new LambdaQueryWrapper<SyncUserLog>()
                .eq(SyncUserLog::getJobId, jobId)
                .ne(SyncUserLog::getStatus, "SUCCESS"));

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
            // 简单设置错误描述，实际可查字典
            vo.setErrorCodeDesc(log.getErrorCode());
            failVOs.add(vo);
        }

        SyncJobDetailVO res = new SyncJobDetailVO();
        res.setJob(toVO(job));
        res.setFailList(failVOs);
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

    // ================= 核心调度逻辑 (重构后) =================

    @Override
    // 移除@Transactional，防止长事务占用数据库连接
    public Long runCfRatingSync(String triggerSource) {
        LocalDateTime start = LocalDateTime.now();
        Long jobId = createJob("RATING_SYNC", triggerSource, start); // 这是一个短事务，很快

        List<UserPlatformAccount> accounts = listAllCfAccounts();

        int total = accounts.size();
        int success = 0, fail = 0, skip = 0;

        for (UserPlatformAccount account : accounts) {
            // 循环内处理单个用户，即使某个用户失败，也不影响其他人，也不回滚已完成的人
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
            // 重新查 Handle
            UserPlatformAccount upa = upaMapper.selectOne(new LambdaQueryWrapper<UserPlatformAccount>()
                    .eq(UserPlatformAccount::getUserId, userId)
                    .eq(UserPlatformAccount::getPlatformId, PLATFORM_CF));

            String status = "SKIP";
            if (upa != null && upa.getIdentifierValue() != null) {
                String handle = upa.getIdentifierValue().trim();
                if ("RATING_SYNC".equals(jobType)) {
                    status = processSingleUserRating(newJobId, userId, handle);
                } else if ("DAILY_SYNC".equals(jobType)) {
                    status = processSingleUserDaily(newJobId, userId, handle, 3); // 重跑默认回溯3天
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

    // ================= 单个用户处理逻辑 (抽取) =================

    // 注意：这个方法内部混合了 网络请求 和 数据库操作。
    // 理想状态是：先 fetch 数据，再调用一个 @Transactional 的 save 方法。
    private String processSingleUserRating(Long jobId, Long userId, String handle) {
        try {
            // 1. 读库（快）
            RatingSnapshot lastSnapshot = ratingSnapshotMapper.selectOne(new LambdaQueryWrapper<RatingSnapshot>()
                    .eq(RatingSnapshot::getUserId, userId)
                    .eq(RatingSnapshot::getPlatformId, PLATFORM_CF)
                    .orderByDesc(RatingSnapshot::getSnapshotTime)
                    .last("LIMIT 1"));

            LocalDateTime lastTime = (lastSnapshot == null) ? null : lastSnapshot.getSnapshotTime();

            // 2. 网络请求（慢！核心阻塞点）—— 此时没有开启事务，不会卡死数据库
            List<CfUserRatingResponse.Item> items = cfClient.getUserRating(handle);

            // 3. 写入数据库
            // 如果需要保证"要么全插入，要么全不插入"，可以将这部分提取为一个单独的 Service 方法加 @Transactional
            // 但考虑到这里是追加日志，即便挂了，下次再跑也能补上，直接循环插入也可。
            int inserted = 0;
            for (CfUserRatingResponse.Item it : items) {
                LocalDateTime t = Instant.ofEpochSecond(it.getRatingUpdateTimeSeconds())
                        .atZone(ZONE_CN).toLocalDateTime();
                if (lastTime != null && !t.isAfter(lastTime)) continue;

                RatingSnapshot snapshot = new RatingSnapshot();
                snapshot.setUserId(userId);
                // ... setters ...
                snapshot.setRating(it.getNewRating());
                // ...
                ratingSnapshotMapper.insert(snapshot); // 这里的单条 insert 是自动 commit 的
                inserted++;
            }

            if (inserted == 0) {
                logUserResult(jobId, userId, "SKIP", "RATING_UNCHANGED", "无新增比赛");
                return "SKIP";
            } else {
                logUserResult(jobId, userId, "SUCCESS", null, "新增 " + inserted + " 条");
                return "SUCCESS";
            }
        } catch (Exception e) {
            // ... 异常处理保持不变 ...
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
            LocalDate minDayForDaily;
            if (firstSync) {
                minDayForDaily = null;
            } else {
                minDayForDaily = today.minusDays(incDays - 1L);
                LocalDate lastDay = lastActivity.getDay();
                if (lastDay.minusDays(1).isBefore(minDayForDaily)) {
                    minDayForDaily = lastDay.minusDays(1);
                }
            }

            Map<LocalDate, Integer> submitCnt = new HashMap<>();
            Map<LocalDate, Integer> acceptCnt = new HashMap<>();
            Map<LocalDate, Set<String>> solvedSet = new HashMap<>();

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
            int newSubmissions = 0, newSolved = 0;

            while (true) {
                List<CfUserStatusResponse.Submission> subs = cfClient.getUserStatus(handle, from, 1000);
                if (subs == null || subs.isEmpty()) break;
                if (from > 10000) break; // 防止死循环

                boolean shouldStop = false;
                for (CfUserStatusResponse.Submission s : subs) {
                    if (s.getId() == null || s.getCreationTimeSeconds() == null) continue;
                    if (!needFull && s.getId() <= lastMaxId) {
                        shouldStop = true;
                        break;
                    }

                    LocalDateTime submitTime = Instant.ofEpochSecond(s.getCreationTimeSeconds()).atZone(ZONE_CN).toLocalDateTime();
                    LocalDate day = submitTime.toLocalDate();

                    // 写入 SubmissionLog
                    int ins = submissionLogMapper.insertIgnore(userId, PLATFORM_CF, handle, s.getId(),
                            (s.getProblem()!=null?s.getProblem().getContestId():null),
                            (s.getProblem()!=null?s.getProblem().getIndex():null),
                            (s.getProblem()!=null?s.getProblem().getName():null),
                            null, s.getVerdict(), submitTime);
                    if (ins > 0) newSubmissions++;

                    // 写入 SolvedProblem
                    if ("OK".equalsIgnoreCase(s.getVerdict()) && s.getProblem() != null && s.getProblem().getContestId() != null) {
                        String key = s.getProblem().getContestId() + "_" + s.getProblem().getIndex();
                        int insSol = solvedProblemMapper.insertIgnore(userId, PLATFORM_CF, handle,
                                s.getProblem().getContestId(), s.getProblem().getIndex(), key,
                                s.getProblem().getName(), null, submitTime);
                        if (insSol > 0) newSolved++;
                    }

                    // 聚合
                    if (!firstSync) {
                        if (day.isBefore(minDayForDaily) || day.isAfter(today)) continue;
                    } else {
                        if (minDayForDaily == null || day.isBefore(minDayForDaily)) minDayForDaily = day;
                    }
                    submitCnt.put(day, submitCnt.getOrDefault(day, 0) + 1);
                    if ("OK".equalsIgnoreCase(s.getVerdict())) {
                        acceptCnt.put(day, acceptCnt.getOrDefault(day, 0) + 1);
                        if (s.getProblem() != null) {
                            String k = s.getProblem().getContestId() + "_" + s.getProblem().getIndex();
                            solvedSet.computeIfAbsent(day, d -> new HashSet<>()).add(k);
                        }
                    }
                }
                if (shouldStop) break;
                from += 1000;
            }

            if (minDayForDaily != null) {
                for (LocalDate d = minDayForDaily; !d.isAfter(today); d = d.plusDays(1)) {
                    // 定义当天的开始和结束时间
                    LocalDateTime dayStart = d.atStartOfDay();
                    LocalDateTime dayEnd = d.plusDays(1).atStartOfDay().minusSeconds(1);

                    // 1. 查询当天的提交总数 (需要你在 SubmissionLogMapper 中加一个 count 方法)
                    // select count(*) from submission_log where user_id=? and platform=1 and submission_time between ? and ?
                    int realSubmitCnt = submissionLogMapper.countByDate(userId, PLATFORM_CF, dayStart, dayEnd);

                    // 2. 查询当天的 AC 数
                    // select count(*) from submission_log where ... and verdict='OK' ...
                    int realAcceptCnt = submissionLogMapper.countAcceptByDate(userId, PLATFORM_CF, dayStart, dayEnd);

                    // 3. 查询当天的去重 AC 题目数 (或者复用你之前的 SolvedProblem 逻辑，但按天统计比较麻烦，这里简化处理)
                    // select count(distinct contest_id, index_id) from submission_log where ... and verdict='OK' ...
                    int realSolvedCnt = submissionLogMapper.countDistinctSolvedByDate(userId, PLATFORM_CF, dayStart, dayEnd);

                    // 4. 使用查出来的真实数据更新
                    dailyActivityMapper.upsert(userId, PLATFORM_CF, handle, d,
                            realSubmitCnt, realAcceptCnt, realSolvedCnt);
                }
            }

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
        uLog.setErrorMessage(msg != null && msg.length() > 500 ? msg.substring(0, 500) : msg);
        uLog.setFetchedAt(LocalDateTime.now());
        syncUserLogMapper.insert(uLog);
    }

    private SyncJobLogVO toVO(SyncJobLog po) {
        if (po == null) return null;
        SyncJobLogVO vo = new SyncJobLogVO();
        BeanUtils.copyProperties(po, vo);
        return vo;
    }

    private void sleepRandom() {
        try { Thread.sleep(300 + new Random().nextInt(500)); } catch (InterruptedException e) {}
    }
}