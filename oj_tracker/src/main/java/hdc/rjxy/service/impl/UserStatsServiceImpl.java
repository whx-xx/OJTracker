package hdc.rjxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hdc.rjxy.mapper.*;
import hdc.rjxy.pojo.Platform;
import hdc.rjxy.pojo.RatingSnapshot;
import hdc.rjxy.pojo.UserPlatformAccount;
import hdc.rjxy.pojo.vo.DailyActivitySummaryVO;
import hdc.rjxy.pojo.vo.RatingPointVO;
import hdc.rjxy.pojo.vo.UserStatsSummaryVO;
import hdc.rjxy.service.UserStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class UserStatsServiceImpl implements UserStatsService {

    @Autowired
    private PlatformMapper platformMapper;
    @Autowired
    private RatingSnapshotMapper ratingSnapshotMapper;
    @Autowired
    private DailyActivityMapper dailyActivityMapper;
    @Autowired
    private UserPlatformAccountMapper upaMapper;
    @Autowired
    private SolvedProblemMapper solvedProblemMapper;

    @Override
    public List<RatingPointVO> ratingHistory(Long userId, String platformCode, Integer limit) {
        if (userId == null) throw new IllegalArgumentException("userId不能为空");
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";

        Platform p = platformMapper.selectOne(new LambdaQueryWrapper<Platform>().eq(Platform::getCode, platformCode));
        if (p == null) throw new IllegalArgumentException("平台不存在");

        // 先查当前绑定的 Handle
        UserPlatformAccount account = upaMapper.selectOne(new LambdaQueryWrapper<UserPlatformAccount>()
                .eq(UserPlatformAccount::getUserId, userId)
                .eq(UserPlatformAccount::getPlatformId, p.getId()));
        if (account == null) return Collections.emptyList(); // 如果没绑定，直接返回空
        String handle = account.getIdentifierValue();


        int lim = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);

        // 查询 Rating 记录，按时间倒序查最后 N 条
        List<RatingSnapshot> snapshots = ratingSnapshotMapper.selectList(new LambdaQueryWrapper<RatingSnapshot>()
                .eq(RatingSnapshot::getUserId, userId)
                .eq(RatingSnapshot::getPlatformId, p.getId())
                .eq(RatingSnapshot::getHandle, handle)
                .orderByDesc(RatingSnapshot::getSnapshotTime)
                .last("LIMIT " + lim));

        if (snapshots.isEmpty()) return Collections.emptyList();

        // 转换并倒序（让前端拿到的是时间正序）
        List<RatingPointVO> result = new ArrayList<>();
        Collections.reverse(snapshots);

        for (RatingSnapshot s : snapshots) {
            RatingPointVO vo = new RatingPointVO();
            vo.setTime(s.getSnapshotTime());
            vo.setRating(s.getRating());
            vo.setContestName(s.getContestName());
            vo.setRank(s.getContestRank());
            vo.setDelta(0);
            result.add(vo);
        }
        return result;
    }

    @Override
    public UserStatsSummaryVO summary(Long userId, String platformCode, int days) {
        if (userId == null) throw new IllegalArgumentException("userId不能为空");
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";
        days = Math.max(1, Math.min(days, 3650));

        Platform p = platformMapper.selectOne(new LambdaQueryWrapper<Platform>().eq(Platform::getCode, platformCode));
        if (p == null) throw new IllegalArgumentException("平台不存在");

        UserPlatformAccount account = upaMapper.selectOne(new LambdaQueryWrapper<UserPlatformAccount>()
                .eq(UserPlatformAccount::getUserId, userId)
                .eq(UserPlatformAccount::getPlatformId, p.getId()));
        if (account == null) throw new IllegalArgumentException("未绑定平台账号");
        String handle = account.getIdentifierValue();

        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);
        LocalDate fromDay = today.minusDays(days - 1L);
        LocalDateTime endTime = today.plusDays(1).atStartOfDay();
        LocalDateTime startTime = fromDay.atStartOfDay();

        // 1. 基础活动汇总 (Submit/Accept/ActiveDays)
        DailyActivitySummaryVO da = dailyActivityMapper.sumByRange(userId, p.getId(), handle, fromDay, today);
        if (da == null) da = new DailyActivitySummaryVO();

        // 2. 解题数统计 (Solved)
        int solvedTotal = solvedProblemMapper.countSolvedInRange(userId, p.getId(), handle, startTime, endTime);

        // 3. 本周解题数 (最近7天)
        LocalDateTime weeklyStart = today.minusDays(6).atStartOfDay();
        int weeklySolved = solvedProblemMapper.countSolvedInRange(userId, p.getId(), handle, weeklyStart, endTime);

        // 4. Rating 首尾数据 (用于计算 Delta)
        RatingSnapshot first = ratingSnapshotMapper.selectOne(new LambdaQueryWrapper<RatingSnapshot>()
                .eq(RatingSnapshot::getUserId, userId)
                .eq(RatingSnapshot::getPlatformId, p.getId())
                .eq(RatingSnapshot::getHandle, handle)
                .ge(RatingSnapshot::getSnapshotTime, startTime)
                .le(RatingSnapshot::getSnapshotTime, endTime)
                .orderByAsc(RatingSnapshot::getSnapshotTime)
                .last("LIMIT 1"));

        RatingSnapshot last = ratingSnapshotMapper.selectOne(new LambdaQueryWrapper<RatingSnapshot>()
                .eq(RatingSnapshot::getUserId, userId)
                .eq(RatingSnapshot::getPlatformId, p.getId())
                .eq(RatingSnapshot::getHandle, handle)
                .ge(RatingSnapshot::getSnapshotTime, startTime)
                .le(RatingSnapshot::getSnapshotTime, endTime)
                .orderByDesc(RatingSnapshot::getSnapshotTime)
                .last("LIMIT 1"));

        // 5. 组装 VO
        UserStatsSummaryVO vo = new UserStatsSummaryVO();
        vo.setPlatformCode(platformCode);
        vo.setDays(days);
        vo.setFrom(fromDay);
        vo.setTo(today);

        vo.setSubmitTotal(da.getSubmitTotal());
        vo.setAcceptTotal(da.getAcceptTotal());
        vo.setActiveDays(da.getActiveDays());
        vo.setAvgSubmitPerDay((double) da.getSubmitTotal() / days);

        vo.setSolvedTotal(solvedTotal);
        vo.setWeeklySolved(weeklySolved);

        if (first != null) vo.setRatingStart(first.getRating());
        if (last != null) {
            vo.setRatingEnd(last.getRating());
            vo.setLastContestName(last.getContestName());
            vo.setLastContestTime(last.getSnapshotTime());
        }
        if (first != null && last != null) {
            vo.setRatingDelta(last.getRating() - first.getRating());
        } else {
            vo.setRatingDelta(0);
        }

        return vo;
    }
}