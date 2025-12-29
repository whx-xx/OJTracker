package hdc.rjxy.service;

import hdc.rjxy.cf.CfUserRatingResponse;
import hdc.rjxy.cf.CfUserStatusResponse;
import hdc.rjxy.cf.CodeforcesClient;
import hdc.rjxy.mapper.*;
import hdc.rjxy.pojo.vo.RatingPointVO;
import hdc.rjxy.pojo.vo.RefreshResultVO;
import hdc.rjxy.pojo.vo.SubmissionTimelineVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserSubmissionService {

    private final PlatformMapper platformMapper;
    private final UserPlatformAccountMapper upaMapper;
    private final SubmissionLogMapper submissionLogMapper;
    private final CodeforcesClient cfClient;

    // 新增依赖：用于处理统计、热力图和AC题目
    private final SolvedProblemMapper solvedProblemMapper;
    private final DailyActivityMapper dailyActivityMapper;
    private final RatingSnapshotMapper ratingSnapshotMapper;

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    public UserSubmissionService(PlatformMapper platformMapper,
                                 UserPlatformAccountMapper upaMapper,
                                 SubmissionLogMapper submissionLogMapper,
                                 CodeforcesClient cfClient,
                                 SolvedProblemMapper solvedProblemMapper,
                                 DailyActivityMapper dailyActivityMapper,
                                 RatingSnapshotMapper ratingSnapshotMapper) {
        this.platformMapper = platformMapper;
        this.upaMapper = upaMapper;
        this.submissionLogMapper = submissionLogMapper;
        this.cfClient = cfClient;
        this.solvedProblemMapper = solvedProblemMapper;
        this.dailyActivityMapper = dailyActivityMapper;
        this.ratingSnapshotMapper = ratingSnapshotMapper;
    }

    /** 时间线：纯读库 (保持不变) */
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
            // 默认 WEEK
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            start = monday.atStartOfDay();
            end = monday.plusDays(7).atStartOfDay();
        }

        return submissionLogMapper.listTimeline(userId, pid, handle, start, end, lim);
    }

    /**
     * 【全量同步模式】
     * 打开页面点击刷新时，循环分页拉取该用户从注册以来的**所有**数据：
     * 1. 提交记录 (Submission) -> 写入 submission_log
     * 2. AC题目 (Solved) -> 写入 solved_problem
     * 3. 每日活跃 (Heatmap) -> 重算受影响日期的 submit/ac 数据 -> 写入 daily_activity
     * 4. 积分 (Rating) -> 写入 rating_snapshot
     */
    @Transactional
    public RefreshResultVO refreshLight(Long userId, String platformCode, Integer count) {
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";
        Long pid = platformMapper.findIdByCode(platformCode);
        if (pid == null) throw new IllegalArgumentException("平台不存在");

        String handle = upaMapper.findIdentifierValue(userId, pid);
        if (handle == null || handle.isBlank()) throw new IllegalArgumentException("未绑定平台账号");
        handle = handle.trim();

        // 1. ================= 全量同步 Rating (积分曲线) =================
        try {
            // Rating 接口一次返回所有历史，直接全量处理
            List<CfUserRatingResponse.Item> ratings = cfClient.getUserRating(handle);

            // 为了防止重复，可以先查一下最后的记录，或者直接利用 insertIgnore/逻辑判断
            // 这里采用简单策略：查最后时间，只插入更新的；如果是首次，全插。
            RatingPointVO lastRating = ratingSnapshotMapper.findLast(userId, pid, handle);
            LocalDateTime lastRatingTime = (lastRating == null) ? null : lastRating.getTime();

            for (CfUserRatingResponse.Item it : ratings) {
                LocalDateTime t = Instant.ofEpochSecond(it.getRatingUpdateTimeSeconds())
                        .atZone(ZONE).toLocalDateTime();

                if (lastRatingTime != null && !t.isAfter(lastRatingTime)) {
                    continue; // 既然是按时间序，旧的跳过
                }
                ratingSnapshotMapper.insert(userId, pid, handle,
                        it.getNewRating(), it.getContestName(), it.getRank(), t);
            }
        } catch (Exception e) {
            e.printStackTrace(); // 此时不应中断后续的提交同步
        }


        // 2. ================= 全量同步 Submissions (热力图 & 统计) =================
        // 忽略前端传入的 count，强制分页拉取直到结束
        int batchSize = 2000; // CF API通常允许较大的页大小
        int from = 1;
        int inserted = 0;
        int totalFetched = 0;

        Set<LocalDate> affectedDays = new HashSet<>(); // 记录所有涉及到的日期

        while (true) {
            // 分页拉取：from=1, count=2000 -> from=2001, count=2000 ...
            List<CfUserStatusResponse.Submission> subs = cfClient.getUserStatus(handle, from, batchSize);

            if (subs == null || subs.isEmpty()) {
                break; // 没有更多数据了，停止
            }
            totalFetched += subs.size();

            for (CfUserStatusResponse.Submission s : subs) {
                Long sec = s.getCreationTimeSeconds();
                if (sec == null) continue;

                LocalDateTime submitTime = Instant.ofEpochSecond(sec).atZone(ZONE).toLocalDateTime();
                LocalDate date = submitTime.toLocalDate();

                // 标记这一天需要重算热力图数据
                affectedDays.add(date);

                Long submissionId = s.getId();
                if (submissionId == null) continue;

                Integer contestId = (s.getProblem() == null) ? null : s.getProblem().getContestId();
                String idx = (s.getProblem() == null) ? null : s.getProblem().getIndex();
                String name = (s.getProblem() == null) ? null : s.getProblem().getName();
                String url = (contestId != null && idx != null)
                        ? ("https://codeforces.com/contest/" + contestId + "/problem/" + idx)
                        : null;

                String verdict = s.getVerdict();

                // 2.1 写入提交记录 (使用 INSERT IGNORE 避免主键冲突)
                int rows = submissionLogMapper.insertIgnore(
                        userId, pid, handle,
                        submissionId,
                        contestId, idx, name, url,
                        verdict, submitTime
                );
                if (rows > 0) inserted++;

                // 2.2 写入 AC 记录 (用于统计 Solved Total)
                if ("OK".equalsIgnoreCase(verdict) && contestId != null && idx != null) {
                    String key = contestId + "_" + idx;
                    solvedProblemMapper.insertIgnore(
                            userId, pid, handle,
                            contestId, idx, key, name, url,
                            submitTime
                    );
                }
            }

            // 如果拉取到的数量少于请求的数量，说明已经是最后一页了
            if (subs.size() < batchSize) {
                break;
            }

            // 翻页
            from += batchSize;

            // 简单防频繁请求 (可选)
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        // 3. ================= 批量修复 Heatmap (Daily Activity) =================
        // 针对所有出现过提交的日期，重新统计当天的 Submit数、AC数、Solved数，并更新到 daily_activity 表
        // 这将修复热力图为灰色/0 的问题
        for (LocalDate day : affectedDays) {
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

            // A. 统计当日去重解题数 (Solved)
            int solved = solvedProblemMapper.countSolvedInRange(userId, pid, handle, dayStart, dayEnd);

            // B. 统计当日提交数 & AC数 (直接查库，因为刚才已经把所有 log 插进去了)
            // 这里 limit 给大一点，确保能算出当天的准确数据
            List<SubmissionTimelineVO> dayLogs = submissionLogMapper.listTimeline(
                    userId, pid, handle, dayStart, dayEnd, 10000);

            int submitCnt = dayLogs.size();
            int acceptCnt = 0;
            for (SubmissionTimelineVO vo : dayLogs) {
                if ("OK".equalsIgnoreCase(vo.getVerdict())) {
                    acceptCnt++;
                }
            }

            // C. 写入/更新统计表
            dailyActivityMapper.upsert(userId, pid, handle, day, submitCnt, acceptCnt, solved);
        }

        RefreshResultVO vo = new RefreshResultVO();
        vo.setFetched(totalFetched);
        vo.setInserted(inserted);
        return vo;
    }
}