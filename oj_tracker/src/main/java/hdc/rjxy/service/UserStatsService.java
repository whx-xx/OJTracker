package hdc.rjxy.service;

import hdc.rjxy.mapper.*;
import hdc.rjxy.pojo.vo.DailyActivitySummaryVO;
import hdc.rjxy.pojo.vo.RatingPointVO;
import hdc.rjxy.pojo.vo.UserStatsSummaryVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class UserStatsService {

    private final PlatformMapper platformMapper;
    private final RatingSnapshotMapper ratingSnapshotMapper;
    private final DailyActivityMapper dailyActivityMapper;
    private final UserPlatformAccountMapper upaMapper;
    private final SolvedProblemMapper solvedProblemMapper;

    public UserStatsService(PlatformMapper platformMapper,
                            RatingSnapshotMapper ratingSnapshotMapper,
                            DailyActivityMapper dailyActivityMapper,
                            UserPlatformAccountMapper upaMapper,
                            SolvedProblemMapper solvedProblemMapper) {
        this.platformMapper = platformMapper;
        this.ratingSnapshotMapper = ratingSnapshotMapper;
        this.dailyActivityMapper = dailyActivityMapper;
        this.upaMapper = upaMapper;
        this.solvedProblemMapper = solvedProblemMapper;
    }

    /**
     * 旧接口兼容：按时间范围查（比如最近10年），再取最后 limit 条
     */
    public List<RatingPointVO> ratingHistory(Long userId, String platformCode, Integer limit) {
        if (userId == null) throw new IllegalArgumentException("userId不能为空");
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";

        int lim = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);

        Long pid = platformMapper.findIdByCode(platformCode);
        if (pid == null) throw new IllegalArgumentException("平台不存在: " + platformCode);

        LocalDateTime end = LocalDateTime.now();
        // 这里给一个“足够大”的范围，避免漏掉历史；数据量大了再优化成SQL里LIMIT
        LocalDateTime start = end.minusYears(10);

        Long platformId = platformMapper.findIdByCode(platformCode);
        String handle = upaMapper.findIdentifierValue(userId, platformId);
        List<RatingPointVO> all = ratingSnapshotMapper.listByTimeRange(userId, pid, handle.trim(),start, end);
        if (all == null || all.isEmpty()) return Collections.emptyList();

        // 取最后 lim 条（因为你一般按 time ASC 返回）
        int n = all.size();
        if (n <= lim) return all;
        return all.subList(n - lim, n);
    }

    public UserStatsSummaryVO summary(Long userId, String platformCode, int days) {
        // 1. 参数校验与默认值处理
        if (userId == null) throw new IllegalArgumentException("userId不能为空");
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";
        days = Math.max(1, Math.min(days, 3650));

        // 2. 获取平台信息
        Long pid = platformMapper.findIdByCode(platformCode);
        if (pid == null) throw new IllegalArgumentException("平台不存在: " + platformCode);

        // 3. 获取绑定的平台账号 (handle)
        String handle = upaMapper.findIdentifierValue(userId, pid);
        if (handle == null || handle.isBlank()) throw new IllegalArgumentException("未绑定平台账号");
        handle = handle.trim();

        // 4. 时间区间计算 (使用上海时区)
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);
        LocalDate fromDay = today.minusDays(days - 1L); // 总统计开始日期

        LocalDateTime endTime = today.plusDays(1).atStartOfDay(); // 统计截止时间 [start, end)

        // 5. 调用 Mapper 执行统计查询

        // 基础活动数据汇总 (提交、通过、活跃天数)
        DailyActivitySummaryVO da = dailyActivityMapper.sumByRange(userId, pid, handle, fromDay, today);

        // 计算指定天数内的总解题数 (solvedTotal)
        LocalDateTime startTime = fromDay.atStartOfDay();
        int solvedTotal = solvedProblemMapper.countSolvedInRange(userId, pid, handle, startTime, endTime);

        // 核心：计算最近 7 天的本周新增 (weeklySolved)
        // 逻辑：从今天算起往前推 6 天，共 7 天数据
        LocalDateTime weeklyStartTime = today.minusDays(6).atStartOfDay();
        int weeklySolved = solvedProblemMapper.countSolvedInRange(userId, pid, handle, weeklyStartTime, endTime);

        // 获取 Rating 变化范围
        RatingPointVO first = ratingSnapshotMapper.findFirstInRange(userId, pid, handle, startTime, endTime);
        RatingPointVO last = ratingSnapshotMapper.findLastInRange(userId, pid, handle, startTime, endTime);

        // 6. 组装并返回 VO
        UserStatsSummaryVO vo = new UserStatsSummaryVO();
        vo.setPlatformCode(platformCode);
        vo.setDays(days);
        vo.setFrom(fromDay);
        vo.setTo(today);

        // 填充活动统计
        vo.setSubmitTotal(da.getSubmitTotal());
        vo.setAcceptTotal(da.getAcceptTotal());
        vo.setActiveDays(da.getActiveDays());
        vo.setAvgSubmitPerDay(days == 0 ? 0.0 : (double) da.getSubmitTotal() / days);

        // 填充去重解题数
        vo.setSolvedTotal(solvedTotal);
        vo.setWeeklySolved(weeklySolved); // 设置周解题数

        // 填充 Rating 信息
        if (first != null) vo.setRatingStart(first.getRating());
        if (last != null) {
            vo.setRatingEnd(last.getRating());
            vo.setLastContestName(last.getContestName());
            vo.setLastContestTime(last.getTime());
        }

        if (first != null && last != null) {
            vo.setRatingDelta(last.getRating() - first.getRating());
        } else {
            vo.setRatingDelta(0);
        }

        return vo;
    }
}
