package hdc.rjxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

/**
 * 数据同步服务实现类
 * <p>
 * 核心职责：
 * 1. 调度与执行 Codeforces 数据的同步任务（Rating 和 提交记录）。
 * 2. 处理增量更新逻辑，避免重复拉取大量数据。
 * 3. 维护同步日志（JobLog 和 UserLog），统计成功/失败率。
 * 4. 提供管理端查询接口，用于监控同步状态和详情。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {

    // Mapper 注入：操作数据库
    private final SyncJobLogMapper syncJobLogMapper;
    private final SyncUserLogMapper syncUserLogMapper;
    private final UserPlatformAccountMapper upaMapper;
    private final UserMapper userMapper;
    private final RatingSnapshotMapper ratingSnapshotMapper;
    private final DailyActivityMapper dailyActivityMapper;
    private final SubmissionLogMapper submissionLogMapper;
    private final SolvedProblemMapper solvedProblemMapper;

    // 第三方客户端注入：调用 Codeforces API
    private final CodeforcesClient cfClient;

    private static final Long PLATFORM_CF = 1L; // 平台 ID 常量：Codeforces
    private static final ZoneId ZONE_CN = ZoneId.of("Asia/Shanghai"); // 时区常量

    // 解析详情字符串的正则: 用于从 errorMessage 字段中提取成功时的统计信息
    // 格式示例: "newSub=10, newSolved=5"
    private static final Pattern DETAIL_PATTERN = Pattern.compile("newSub=(\\d+), newSolved=(\\d+)");

    // ================= 管理端查询接口 =================

    /**
     * 分页查询同步任务日志
     *
     * @param page      当前页码
     * @param pageSize  每页大小
     * @param jobType   任务类型筛选 (如 "RATING_SYNC", "DAILY_SYNC")
     * @return 任务日志 VO 分页对象
     */
    @Override
    public Page<SyncJobLogVO> pageJobs(int page, int pageSize, String jobType) {
        // 1. 查询 PO 分页 (MyBatis-Plus)
        Page<SyncJobLog> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<SyncJobLog> wrapper = new LambdaQueryWrapper<>();
        if (jobType != null && !jobType.isBlank()) {
            wrapper.eq(SyncJobLog::getJobType, jobType);
        }
        // 按开始时间倒序排列，最新的在前
        wrapper.orderByDesc(SyncJobLog::getStartTime);

        syncJobLogMapper.selectPage(p, wrapper);

        // 2. 将 PO 转换为 VO 返回给前端
        Page<SyncJobLogVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        List<SyncJobLogVO> vos = p.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        voPage.setRecords(vos);
        return voPage;
    }

    /**
     * 获取单个同步任务的详细执行情况
     * 包括：任务基本信息、失败用户列表、有数据变更的用户列表
     *
     * @param jobId 任务 ID
     * @return 任务详情 VO
     */
    @Override
    public SyncJobDetailVO jobDetail(Long jobId) {
        SyncJobLog job = syncJobLogMapper.selectById(jobId);
        if (job == null) throw new IllegalArgumentException("Job not found");

        // 1. 处理失败列表
        // 排除 SUCCESS 和 SKIP 状态，剩下的即为 FAIL 或 PARTIAL_FAIL
        List<SyncUserLog> failLogs = syncUserLogMapper.selectList(new LambdaQueryWrapper<SyncUserLog>()
                .eq(SyncUserLog::getJobId, jobId)
                .ne(SyncUserLog::getStatus, "SUCCESS")
                .ne(SyncUserLog::getStatus, "SKIP"));

        List<SyncUserFailVO> failVOs = new ArrayList<>();

        for (SyncUserLog log : failLogs) {
            SyncUserFailVO vo = new SyncUserFailVO();
            BeanUtils.copyProperties(log, vo);

            // 补充用户 Handle (因为 Log 表只存了 UserId)
            UserPlatformAccount upa = upaMapper.selectOne(new LambdaQueryWrapper<UserPlatformAccount>()
                    .eq(UserPlatformAccount::getUserId, log.getUserId())
                    .eq(UserPlatformAccount::getPlatformId, log.getPlatformId()));
            if (upa != null) {
                vo.setHandle(upa.getIdentifierValue());
            }
            vo.setErrorCodeDesc(log.getErrorCode()); // 错误码描述
            failVOs.add(vo);
        }

        // 2. 处理变更列表 (成功且有数据变化的记录)
        List<SyncUserChangeVO> changeList = new ArrayList<>();

        // 查询所有成功的记录
        List<SyncUserLog> successLogs = syncUserLogMapper.selectList(new LambdaQueryWrapper<SyncUserLog>()
                .eq(SyncUserLog::getJobId, jobId)
                .eq(SyncUserLog::getStatus, "SUCCESS"));

        List<SyncUserChangeVO> tempChanges = new ArrayList<>();
        Set<Long> successUserIds = new HashSet<>();

        for (SyncUserLog l : successLogs) {
            // 注意：因为数据库没有设计专门的 details 字段，为了节省空间，
            // 成功时的统计数据（新增提交数、新增解题数）复用了 errorMessage 字段存储
            String info = l.getErrorMessage();
            if (info == null || info.isEmpty()) continue;

            // 正则解析 "newSub=10, newSolved=5"
            Matcher m = DETAIL_PATTERN.matcher(info);
            if (m.find()) {
                try {
                    int newSub = Integer.parseInt(m.group(1));
                    int newSolved = Integer.parseInt(m.group(2));

                    // 只有当有新提交或新解决时才加入变更列表展示
                    if (newSub > 0 || newSolved > 0) {
                        SyncUserChangeVO vo = new SyncUserChangeVO();
                        vo.setUserId(l.getUserId());
                        vo.setNewSub(newSub);
                        vo.setNewSolved(newSolved);
                        vo.setRawDetails(info); // 保存原始信息

                        tempChanges.add(vo);
                        successUserIds.add(l.getUserId()); // 记录 ID 用于批量查 Handle
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // 批量查询变更用户的 Handle，避免循环查库
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

        // 3. 组装最终结果
        SyncJobDetailVO res = new SyncJobDetailVO();
        res.setJob(toVO(job));
        res.setFailList(failVOs);
        res.setChangeList(changeList);
        return res;
    }

    /**
     * 获取同步概览信息
     * 用于 Dashboard 展示最近一次同步状态和历史记录
     */
    @Override
    public SyncOverviewVO overview(int recentLimit) {
        SyncOverviewVO vo = new SyncOverviewVO();

        // 获取最新的一次 Rating 同步任务
        SyncJobLog ratingJob = syncJobLogMapper.selectOne(new LambdaQueryWrapper<SyncJobLog>()
                .eq(SyncJobLog::getJobType, "RATING_SYNC")
                .orderByDesc(SyncJobLog::getStartTime)
                .last("LIMIT 1"));
        vo.setLatestRating(toVO(ratingJob));

        // 获取最新的一次 日常(提交) 同步任务
        SyncJobLog dailyJob = syncJobLogMapper.selectOne(new LambdaQueryWrapper<SyncJobLog>()
                .eq(SyncJobLog::getJobType, "DAILY_SYNC")
                .orderByDesc(SyncJobLog::getStartTime)
                .last("LIMIT 1"));
        vo.setLatestDaily(toVO(dailyJob));

        // 获取最近的 N 条记录列表
        Page<SyncJobLog> p = new Page<>(1, recentLimit);
        syncJobLogMapper.selectPage(p, new LambdaQueryWrapper<SyncJobLog>().orderByDesc(SyncJobLog::getStartTime));
        vo.setRecent(p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));

        return vo;
    }

    // ================= 核心调度逻辑 =================

    /**
     * 执行 Rating 同步任务
     * 遍历所有绑定的 CF 账号，更新其 Rating 历史
     */
    @Override
    public Long runCfRatingSync(String triggerSource) {
        LocalDateTime start = LocalDateTime.now();
        // 1. 创建任务记录，状态 RUNNING
        Long jobId = createJob("RATING_SYNC", triggerSource, start);

        // 2. 获取所有绑定了 CF 的账号
        List<UserPlatformAccount> accounts = listAllCfAccounts();

        int total = accounts.size();
        int success = 0, fail = 0, skip = 0;

        // 3. 串行处理每个用户 (为了避免触发 CF 的 QPS 限制)
        for (UserPlatformAccount account : accounts) {
            String res = processSingleUserRating(jobId, account.getUserId(), account.getIdentifierValue());

            // 统计状态
            if ("SUCCESS".equals(res)) success++;
            else if ("SKIP".equals(res)) skip++;
            else fail++;

            // 4. 随机休眠 (300ms ~ 800ms)，防止 IP 被封
            sleepRandom();
        }

        // 5. 任务结束，更新统计数据
        finishJob(jobId, start, total, success, fail, skip);
        return jobId;
    }

    /**
     * 执行日常提交记录同步任务
     * @param days 回溯天数 (增量更新检查的时间范围)
     */
    @Override
    public Long runCfDailySync(String triggerSource, int days) {
        LocalDateTime start = LocalDateTime.now();
        Long jobId = createJob("DAILY_SYNC", triggerSource, start);

        List<UserPlatformAccount> accounts = listAllCfAccounts();
        // 限制回溯天数在 2-7 天之间，防止参数过大导致性能问题
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

    /**
     * 重试失败的用户
     * 针对指定的旧 Job ID，找出其中失败或部分失败的用户，重新创建一个新任务进行同步
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long rerunFailedUsers(Long oldJobId, String triggerSource) {
        SyncJobLog oldJob = syncJobLogMapper.selectById(oldJobId);
        if (oldJob == null) throw new IllegalArgumentException("Original job not found");

        String jobType = oldJob.getJobType();
        LocalDateTime start = LocalDateTime.now();
        // 创建一个新的 Job 记录重跑过程
        Long newJobId = createJob(jobType, triggerSource, start);

        // 1. 查出旧任务中所有非 SUCCESS/SKIP 的用户日志
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

        // 2. 遍历失败用户进行重试
        for (SyncUserLog failLog : fails) {
            Long userId = failLog.getUserId();
            // 重新查询账号信息，防止用户已解绑
            UserPlatformAccount upa = upaMapper.selectOne(new LambdaQueryWrapper<UserPlatformAccount>()
                    .eq(UserPlatformAccount::getUserId, userId)
                    .eq(UserPlatformAccount::getPlatformId, PLATFORM_CF));

            String status = "SKIP";
            if (upa != null && upa.getIdentifierValue() != null) {
                String handle = upa.getIdentifierValue().trim();
                // 根据任务类型调用对应的处理逻辑
                if ("RATING_SYNC".equals(jobType)) {
                    status = processSingleUserRating(newJobId, userId, handle);
                } else if ("DAILY_SYNC".equals(jobType)) {
                    // 重试时默认回溯 3 天
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

    /**
     * 单个用户的 Rating 同步逻辑
     */
    private String processSingleUserRating(Long jobId, Long userId, String handle) {
        try {
            // 1. 查询本地数据库中该用户最后一次 Rating 记录的时间
            RatingSnapshot lastSnapshot = ratingSnapshotMapper.selectOne(new LambdaQueryWrapper<RatingSnapshot>()
                    .eq(RatingSnapshot::getUserId, userId)
                    .eq(RatingSnapshot::getPlatformId, PLATFORM_CF)
                    .orderByDesc(RatingSnapshot::getSnapshotTime)
                    .last("LIMIT 1"));

            LocalDateTime lastTime = (lastSnapshot == null) ? null : lastSnapshot.getSnapshotTime();

            // 2. 调用 CF API 获取用户的所有 Rating 历史
            List<CfUserRatingResponse.Item> items = cfClient.getUserRating(handle);

            int inserted = 0;
            for (CfUserRatingResponse.Item it : items) {
                // 转换时间戳
                LocalDateTime t = Instant.ofEpochSecond(it.getRatingUpdateTimeSeconds())
                        .atZone(ZONE_CN).toLocalDateTime();

                // 3. 增量判断：如果 API 返回的时间不晚于本地最后时间，则跳过
                if (lastTime != null && !t.isAfter(lastTime)) continue;

                // 4. 插入新记录
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
                // 成功时将统计数据写入 errorMessage 字段以便 jobDetail 解析
                logUserResult(jobId, userId, "SUCCESS", null, "newSub=0, newSolved=0, ratingChanges=" + inserted);
                return "SUCCESS";
            }
        } catch (Exception e) {
            log.error("Rating sync fail user={}", userId, e);
            logUserResult(jobId, userId, "FAIL", "UNKNOWN", e.getMessage());
            return "FAIL";
        }
    }

    /**
     * 单个用户的提交记录与日常活跃度同步逻辑
     */
    private String processSingleUserDaily(Long jobId, Long userId, String handle, int incDays) {
        LocalDate today = LocalDate.now(ZONE_CN);
        try {
            // 查询最后一次活跃记录，用于判断是否是首次同步
            DailyActivity lastActivity = dailyActivityMapper.selectOne(new LambdaQueryWrapper<DailyActivity>()
                    .eq(DailyActivity::getUserId, userId)
                    .eq(DailyActivity::getPlatformId, PLATFORM_CF)
                    .orderByDesc(DailyActivity::getDay)
                    .last("LIMIT 1"));

            boolean firstSync = (lastActivity == null);
            Long lastMaxId = null;

            // 查询本地最大的 submissionId，用于增量截止判断
            SubmissionLog maxLog = submissionLogMapper.selectOne(new LambdaQueryWrapper<SubmissionLog>()
                    .select(SubmissionLog::getSubmissionId)
                    .eq(SubmissionLog::getUserId, userId)
                    .eq(SubmissionLog::getPlatformId, PLATFORM_CF)
                    .orderByDesc(SubmissionLog::getSubmissionId)
                    .last("LIMIT 1"));
            if (maxLog != null) lastMaxId = maxLog.getSubmissionId();

            // 如果没有历史记录，说明需要全量同步
            boolean needFull = (lastMaxId == null);
            int from = 1;
            int count = needFull ? 2000 : 10; // 全量同步每次拉 2000，增量默认拉 10 (会动态调整)
            int newSubmissions = 0, newSolved = 0;
            LocalDate firstSubmitDateInThisBatch = null; // 本次同步中最早的一条提交日期

            // 分页拉取 CF 提交记录
            while (true) {
                List<CfUserStatusResponse.Submission> subs = cfClient.getUserStatus(handle, from, count);

                if (subs == null || subs.isEmpty()) break;
                // 安全熔断：如果拉取深度超过 100万，强制停止，防止死循环
                if (from > 1000000) {
                    log.warn("User {} submissions exceed limit 1,000,000, stop syncing.", userId);
                    break;
                }

                boolean shouldStop = false;
                for (CfUserStatusResponse.Submission s : subs) {
                    if (s.getId() == null || s.getCreationTimeSeconds() == null) continue;

                    // 【核心增量逻辑】
                    // 如果不是全量同步，且当前提交ID <= 本地最大ID，说明接下来的都是旧数据，停止处理
                    if (!needFull && s.getId() <= lastMaxId) {
                        shouldStop = true;
                        break;
                    }

                    LocalDateTime submitTime = Instant.ofEpochSecond(s.getCreationTimeSeconds()).atZone(ZONE_CN).toLocalDateTime();
                    LocalDate day = submitTime.toLocalDate();

                    // 记录本次批次中遇到的最早日期
                    if (firstSubmitDateInThisBatch == null || day.isBefore(firstSubmitDateInThisBatch)) {
                        firstSubmitDateInThisBatch = day;
                    }

                    // 构建插入数据
                    Integer rating = (s.getProblem() != null) ? s.getProblem().getRating() : null;
                    Integer contestId = (s.getProblem() == null) ? null : s.getProblem().getContestId();
                    String idx = (s.getProblem() == null) ? null : s.getProblem().getIndex();
                    String name = (s.getProblem() == null) ? null : s.getProblem().getName();
                    String url = (contestId != null && idx != null)
                            ? ("https://codeforces.com/contest/" + contestId + "/problem/" + idx) : null;

                    // 插入 SubmissionLog (忽略重复)
                    int ins = submissionLogMapper.insertIgnore(userId, PLATFORM_CF, handle, s.getId(),
                            contestId, idx, name, url, s.getVerdict(), rating, submitTime);
                    if (ins > 0) newSubmissions++;

                    // 如果 AC 了，更新 SolvedProblem 表
                    if ("OK".equalsIgnoreCase(s.getVerdict()) && s.getProblem() != null && s.getProblem().getContestId() != null) {
                        String key = s.getProblem().getContestId() + "_" + s.getProblem().getIndex();
                        int insSol = solvedProblemMapper.insertIgnore(userId, PLATFORM_CF, handle,
                                contestId, idx, key, name, url, rating, submitTime);
                        if (insSol > 0) newSolved++;
                    }
                }

                if (shouldStop) break;

                // 翻页逻辑
                from += count;
                // 如果是增量但没触发停止（说明新题很多），自动扩大步长加速拉取
                if (count < 2000) count = 2000;
            }

            // --- 重新计算每日统计数据 (DailyActivity) ---
            LocalDate minDayForDaily = null;
            if (firstSync) {
                // 首次同步：从拉取到的最早日期开始算
                minDayForDaily = firstSubmitDateInThisBatch;
            } else {
                // 增量同步：默认回溯 incDays 天
                minDayForDaily = today.minusDays(incDays - 1L);
                // 如果有新提交，且回溯日期晚于最后活跃日期，则从最后活跃日期的前一天开始重算，防止遗漏
                if (newSubmissions > 0 && lastActivity != null) {
                    LocalDate lastDay = lastActivity.getDay();
                    if (lastDay.minusDays(1).isBefore(minDayForDaily)) {
                        minDayForDaily = lastDay.minusDays(1);
                    }
                }
            }

            // 批量更新每一天的统计数据
            if (minDayForDaily != null) {
                if (minDayForDaily.isAfter(today)) minDayForDaily = today;
                for (LocalDate d = minDayForDaily; !d.isAfter(today); d = d.plusDays(1)) {
                    LocalDateTime dayStart = d.atStartOfDay();
                    LocalDateTime dayEnd = d.plusDays(1).atStartOfDay().minusSeconds(1);

                    // 查库统计当天的真实数据
                    int realSubmitCnt = submissionLogMapper.countByDate(userId, PLATFORM_CF, dayStart, dayEnd);
                    int realAcceptCnt = submissionLogMapper.countAcceptByDate(userId, PLATFORM_CF, dayStart, dayEnd);
                    int realSolvedCnt = submissionLogMapper.countDistinctSolvedByDate(userId, PLATFORM_CF, dayStart, dayEnd);

                    // 插入或更新每日统计表
                    dailyActivityMapper.upsert(userId, PLATFORM_CF, handle, d,
                            realSubmitCnt, realAcceptCnt, realSolvedCnt);
                }
            }

            // 成功：记录统计信息
            logUserResult(jobId, userId, "SUCCESS", null, "newSub=" + newSubmissions + ", newSolved=" + newSolved);
            return "SUCCESS";

        } catch (Exception e) {
            log.error("Daily sync fail user={}", userId, e);
            logUserResult(jobId, userId, "FAIL", "UNKNOWN", e.getMessage());
            return "FAIL";
        }
    }

    // ================= 辅助方法 =================

    /**
     * 列出所有绑定的 Codeforces 账号
     */
    private List<UserPlatformAccount> listAllCfAccounts() {
        return upaMapper.selectList(new LambdaQueryWrapper<UserPlatformAccount>()
                .eq(UserPlatformAccount::getPlatformId, PLATFORM_CF)
                .isNotNull(UserPlatformAccount::getIdentifierValue)
                .ne(UserPlatformAccount::getIdentifierValue, ""));
    }

    /**
     * 创建一个新的 Job 记录
     */
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

    /**
     * 结束 Job，更新统计信息
     */
    private void finishJob(Long jobId, LocalDateTime start, int total, int success, int fail, int skip) {
        LocalDateTime end = LocalDateTime.now();
        SyncJobLog job = new SyncJobLog();
        job.setId(jobId);
        job.setEndTime(end);
        job.setDurationMs(Duration.between(start, end).toMillis()); // 计算耗时
        job.setTotalCount(total);
        job.setSuccessCount(success);
        job.setFailCount(fail);
        // 在 message 中记录简报
        job.setMessage(String.format("S=%d, F=%d, K=%d", success, fail, skip));
        // 设置最终状态
        job.setStatus(fail == 0 ? "SUCCESS" : (success == 0 ? "FAIL" : "PARTIAL_FAIL"));
        syncJobLogMapper.updateById(job);
    }

    /**
     * 记录单个用户的同步结果 (SyncUserLog)
     */
    private void logUserResult(Long jobId, Long userId, String status, String errCode, String msg) {
        SyncUserLog uLog = new SyncUserLog();
        uLog.setJobId(jobId);
        uLog.setUserId(userId);
        uLog.setPlatformId(PLATFORM_CF);
        uLog.setStatus(status);
        uLog.setErrorCode(errCode);

        // 无论成功还是失败，都将 msg 写入 errorMessage 字段
        // 成功时 msg 包含 "newSub=X, newSolved=Y"
        // 失败时 msg 包含 异常堆栈或错误描述
        uLog.setErrorMessage(msg != null && msg.length() > 500 ? msg.substring(0, 500) : msg);

        uLog.setFetchedAt(LocalDateTime.now());
        syncUserLogMapper.insert(uLog);
    }

    /**
     * 实体对象转 VO
     */
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

    /**
     * 随机休眠 helper
     * 避免高频请求触发 CF API 限制 (Error 429 / 503)
     */
    private void sleepRandom() {
        try { Thread.sleep(300 + new Random().nextInt(500)); } catch (InterruptedException e) {}
    }
}