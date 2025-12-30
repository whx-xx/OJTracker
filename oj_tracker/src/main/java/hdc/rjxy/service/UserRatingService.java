package hdc.rjxy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.RatingSnapshotMapper;
import hdc.rjxy.mapper.UserPlatformAccountMapper;
import hdc.rjxy.pojo.Platform;
import hdc.rjxy.pojo.RatingSnapshot;
import hdc.rjxy.pojo.UserPlatformAccount;
import hdc.rjxy.pojo.vo.RatingHistoryPointVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserRatingService {

    @Autowired
    private UserPlatformAccountMapper upaMapper;
    @Autowired
    private PlatformMapper platformMapper;
    @Autowired
    private RatingSnapshotMapper ratingSnapshotMapper;

    public List<RatingHistoryPointVO> history(Long userId, String platformCode, int days) {
        if (platformCode == null || platformCode.isBlank()) platformCode = "CF";
        Platform p = platformMapper.selectOne(new LambdaQueryWrapper<Platform>().eq(Platform::getCode, platformCode));
        if (p == null) throw new IllegalArgumentException("平台不存在");

        UserPlatformAccount account = upaMapper.selectOne(new LambdaQueryWrapper<UserPlatformAccount>()
                .eq(UserPlatformAccount::getUserId, userId)
                .eq(UserPlatformAccount::getPlatformId, p.getId()));
        if (account == null) return new ArrayList<>();

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days - 1L);

        // 使用 MP 查询
        List<RatingSnapshot> snapshots = ratingSnapshotMapper.selectList(new LambdaQueryWrapper<RatingSnapshot>()
                .eq(RatingSnapshot::getUserId, userId)
                .eq(RatingSnapshot::getPlatformId, p.getId())
                .ge(RatingSnapshot::getSnapshotTime, start)
                .le(RatingSnapshot::getSnapshotTime, end)
                .orderByAsc(RatingSnapshot::getSnapshotTime));

        List<RatingHistoryPointVO> res = new ArrayList<>();
        Integer prevRating = null;

        for (RatingSnapshot s : snapshots) {
            RatingHistoryPointVO vo = new RatingHistoryPointVO();
            vo.setTime(s.getSnapshotTime());
            vo.setRating(s.getRating());
            vo.setContestName(s.getContestName());
            vo.setRank(s.getContestRank());

            // 计算 Delta
            vo.setDelta(prevRating == null ? 0 : s.getRating() - prevRating);
            prevRating = s.getRating();

            res.add(vo);
        }
        return res;
    }
}