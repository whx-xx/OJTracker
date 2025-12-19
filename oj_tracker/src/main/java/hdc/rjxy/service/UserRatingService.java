package hdc.rjxy.service;

import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.RatingSnapshotMapper;
import hdc.rjxy.mapper.UserPlatformAccountMapper;
import hdc.rjxy.pojo.vo.RatingHistoryPointVO;
import hdc.rjxy.pojo.vo.RatingPointVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserRatingService {

    private final UserPlatformAccountMapper upaMapper;
    private final PlatformMapper platformMapper;
    private final RatingSnapshotMapper ratingSnapshotMapper;

    public UserRatingService(UserPlatformAccountMapper upaMapper,
                             PlatformMapper platformMapper,
                             RatingSnapshotMapper ratingSnapshotMapper) {
        this.upaMapper = upaMapper;
        this.platformMapper = platformMapper;
        this.ratingSnapshotMapper = ratingSnapshotMapper;
    }

    public List<RatingHistoryPointVO> history(Long userId,
                                              String platformCode,
                                              int days) {

        Long platformId = platformMapper.findIdByCode(platformCode);
        if (platformId == null) {
            throw new IllegalArgumentException("平台不存在");
        }

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days - 1);

        String handle = upaMapper.findIdentifierValue(userId, platformId);
        if (handle == null) {
            return new ArrayList<>();
        }

        List<RatingPointVO> list =
                ratingSnapshotMapper.listByTimeRange(
                        userId, platformId, handle.trim(), start, end);

        List<RatingHistoryPointVO> res = new ArrayList<>();

        Integer prev = null;
        for (RatingPointVO p : list) {
            RatingHistoryPointVO vo = new RatingHistoryPointVO();
            vo.setTime(p.getTime());
            vo.setRating(p.getRating());
            vo.setContestName(p.getContestName());

            vo.setRank(p.getRank());

            vo.setDelta(prev == null ? 0 : p.getRating() - prev);
            prev = p.getRating();
            res.add(vo);
        }
        return res;
    }
}