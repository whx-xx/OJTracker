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

        // 准备一个空的分页对象，如果校验失败直接返回空
        Page<SubmissionTimelineVO> resultPage = new Page<>(pageNum, pageSize);
        if (p == null) return resultPage;

        // 2. 获取用户绑定的 Handle
        UserPlatformAccount account = upaMapper.selectOne(new LambdaQueryWrapper<UserPlatformAccount>()
                .eq(UserPlatformAccount::getUserId, userId)
                .eq(UserPlatformAccount::getPlatformId, p.getId()));

        if (account == null) return resultPage;
        String handle = account.getIdentifierValue();

        // 3. 计算时间范围
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime start = null;
        LocalDateTime end = null;

        if ("TODAY".equalsIgnoreCase(range)) {
            start = today.atStartOfDay();
            end = today.plusDays(1).atStartOfDay();
        } else if ("WEEK".equalsIgnoreCase(range)) {
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            start = monday.atStartOfDay();
            end = monday.plusDays(7).atStartOfDay();
        } else if ("MONTH".equalsIgnoreCase(range)) {
            LocalDate firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate firstDayOfNextMonth = today.with(TemporalAdjusters.firstDayOfNextMonth());
            start = firstDayOfMonth.atStartOfDay();
            end = firstDayOfNextMonth.atStartOfDay();
        }

        // 4. 处理关键词 (空字符串转 null，方便 SQL 判断)
        String searchKey = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        // 5. 执行自定义查询 (Mapper 中使用 LEFT JOIN 关联 solved_problem 表获取 tags)
        // selectTimelineWithTags 返回的是 IPage，直接强转即可
        return (Page<SubmissionTimelineVO>) submissionLogMapper.selectTimelineWithTags(
                resultPage,
                userId,
                p.getId(),
                handle,
                start,
                end,
                searchKey
        );
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
                    .eq(RatingSnapshot::getHandle, handle)
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

        // 3. 同步 Submissions (优化版)

        // 获取本地已有的最新提交ID，用于断点判断
        Long lastMaxId = null;
        SubmissionLog maxLog = submissionLogMapper.selectOne(new LambdaQueryWrapper<SubmissionLog>()
                .select(SubmissionLog::getSubmissionId)
                .eq(SubmissionLog::getUserId, userId)
                .eq(SubmissionLog::getPlatformId, p.getId())
                .eq(SubmissionLog::getHandle, handle)
                .orderByDesc(SubmissionLog::getSubmissionId)
                .last("LIMIT 1"));
        if (maxLog != null) lastMaxId = maxLog.getSubmissionId();

        // 如果没有本地记录，说明是首次全量同步，不应该受 count 参数的小限制（除非 count 是强制指定的小值）
        // 这里逻辑设定：如果 count <= 0 或 null，且是首次同步，则视为无限；如果 count > 0，则视为用户强制限制。
        boolean unlimited = (count == null || count <= 0);
        int limit = unlimited ? 1000000 : count; // 默认上限 100万，防止死循环

        int batchSize = 2000; // CF 推荐的大页抓取
        int from = 1;
        int inserted = 0;
        int totalFetched = 0;
        Set<LocalDate> affectedDays = new HashSet<>();

        while (true) {
            List<CfUserStatusResponse.Submission> subs = cfClient.getUserStatus(handle, from, batchSize);
            if (subs == null || subs.isEmpty()) break;

            boolean shouldStop = false;

            for (CfUserStatusResponse.Submission s : subs) {
                // [优化] 增量同步核心逻辑：一旦遇到比本地最新ID更旧的记录，直接停止处理后续数据
                // 只有在非强制全量重跑（unlimited）或者正常增量更新时生效
                if (lastMaxId != null && s.getId() <= lastMaxId) {
                    shouldStop = true;
                    break;
                }

                Long sec = s.getCreationTimeSeconds();
                if (sec == null) continue;
                LocalDateTime submitTime = Instant.ofEpochSecond(sec).atZone(ZONE).toLocalDateTime();
                affectedDays.add(submitTime.toLocalDate());

                Integer contestId = (s.getProblem() == null) ? null : s.getProblem().getContestId();
                String idx = (s.getProblem() == null) ? null : s.getProblem().getIndex();
                String name = (s.getProblem() == null) ? null : s.getProblem().getName();
                Integer rating = (s.getProblem() == null) ? null : s.getProblem().getRating();
                String url = (contestId != null && idx != null)
                        ? ("https://codeforces.com/contest/" + contestId + "/problem/" + idx) : null;

                // 使用 insertIgnore 避免主键冲突
                int rows = submissionLogMapper.insertIgnore(userId, p.getId(), handle, s.getId(),
                        contestId, idx, name, url, s.getVerdict(), rating, submitTime);
                if (rows > 0) inserted++;

                if ("OK".equalsIgnoreCase(s.getVerdict()) && contestId != null && idx != null) {
                    String key = contestId + "_" + idx;
                    solvedProblemMapper.insertIgnore(userId, p.getId(), handle,
                            contestId, idx, key, name, url, rating, submitTime);
                }
            }

            totalFetched += subs.size();

            // 停止条件 1: 触发增量断点
            if (shouldStop) break;
            // 停止条件 2: 超过了用户指定的最大抓取数
            if (totalFetched >= limit) break;
            // 停止条件 3: API 返回数据不足一页，说明已经到底了
            if (subs.size() < batchSize) break;

            from += batchSize;
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        // 4. 重算热力图
        // 注意：如果是首次全量同步，affectedDays 可能非常多（数千天），这里循环查询可能会稍慢。
        // 但考虑到是单用户操作，通常可以接受。如果需要极致优化，可以改写为批量 SQL。
        for (LocalDate day : affectedDays) {
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

            int solved = solvedProblemMapper.countSolvedInRange(userId, p.getId(), handle, dayStart, dayEnd);

            Long submitCnt = submissionLogMapper.selectCount(new LambdaQueryWrapper<SubmissionLog>()
                    .eq(SubmissionLog::getUserId, userId)
                    .eq(SubmissionLog::getPlatformId, p.getId())
                    .eq(SubmissionLog::getHandle, handle)
                    .ge(SubmissionLog::getSubmitTime, dayStart)
                    .lt(SubmissionLog::getSubmitTime, dayEnd));

            Long acceptCnt = submissionLogMapper.selectCount(new LambdaQueryWrapper<SubmissionLog>()
                    .eq(SubmissionLog::getUserId, userId)
                    .eq(SubmissionLog::getPlatformId, p.getId())
                    .eq(SubmissionLog::getHandle, handle)
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