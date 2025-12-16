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
        if (userId == null) throw new IllegalArgumentException("userId不能为空");
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";
        days = Math.max(1, Math.min(days, 3650));

        Long pid = platformMapper.findIdByCode(platformCode);
        if (pid == null) throw new IllegalArgumentException("平台不存在: " + platformCode);

        String handle = upaMapper.findIdentifierValue(userId, pid);
        if (handle == null || handle.isBlank()) throw new IllegalArgumentException("未绑定平台账号");
        handle = handle.trim();

        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);
        LocalDate fromDay = today.minusDays(days - 1L);

        // daily_activity：按天汇总
        DailyActivitySummaryVO da =
                dailyActivityMapper.sumByRange(userId, pid, handle, fromDay, today);

        //  solvedTotal：改为 solved_problem 去重统计（按 first_ac_time 落在区间）
        LocalDateTime startTime = fromDay.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay(); // [start, end)
        int solvedTotal = solvedProblemMapper.countSolvedInRange(userId, pid, handle, startTime, endTime);

        // rating：你之前怎么做就怎么做（示例保留）
        RatingPointVO first = ratingSnapshotMapper.findFirstInRange(userId, pid, handle, startTime, endTime);
        RatingPointVO last = ratingSnapshotMapper.findLastInRange(userId, pid, handle, startTime, endTime);

        UserStatsSummaryVO vo = new UserStatsSummaryVO();
        vo.setPlatformCode(platformCode);
        vo.setDays(days);
        vo.setFrom(fromDay);
        vo.setTo(today);

        vo.setSubmitTotal(da.getSubmitTotal());
        vo.setAcceptTotal(da.getAcceptTotal());
        vo.setActiveDays(da.getActiveDays());
        vo.setAvgSubmitPerDay(days == 0 ? 0.0 : (double) da.getSubmitTotal() / days);

        vo.setSolvedTotal(solvedTotal); //  新口径

        // rating部分（你 VO 字段叫什么就按你字段 set）
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
