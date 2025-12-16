package hdc.rjxy.service;

import hdc.rjxy.mapper.DailyActivityMapper;
import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.UserPlatformAccountMapper;
import hdc.rjxy.pojo.vo.HeatmapDayVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class UserActivityService {

    private final DailyActivityMapper dailyActivityMapper;
    private final PlatformMapper platformMapper;
    private final UserPlatformAccountMapper upaMapper;

    public UserActivityService(DailyActivityMapper dailyActivityMapper, PlatformMapper platformMapper, UserPlatformAccountMapper upaMapper) {
        this.dailyActivityMapper = dailyActivityMapper;
        this.platformMapper = platformMapper;
        this.upaMapper = upaMapper;
    }

    public List<HeatmapDayVO> heatmap(Long userId, String platformCode, int days) {
        if (platformCode == null || platformCode.trim().isEmpty()) {
            throw new IllegalArgumentException("platformCode不能为空");
        }
        if (days <= 0 || days > 365) {
            throw new IllegalArgumentException("days范围建议 1~365");
        }

        Long platformId = platformMapper.findIdByCode(platformCode.trim());
        if (platformId == null) throw new IllegalArgumentException("平台不存在: " + platformCode);

        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);
        LocalDate start = today.minusDays(days - 1L);

        // DB 里可能缺一些天（没同步/没写0），这里补齐，让前端渲染更舒服
        String handle = upaMapper.findIdentifierValue(userId, platformId);
        List<HeatmapDayVO> rows = dailyActivityMapper.listHeatmap(userId, platformId, handle.trim(), start, today);

        Map<LocalDate, HeatmapDayVO> map = new HashMap<>();
        for (HeatmapDayVO r : rows) {
            map.put(r.getDay(), r);
        }

        List<HeatmapDayVO> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            HeatmapDayVO vo = map.get(d);
            if (vo == null) {
                vo = new HeatmapDayVO();
                vo.setDay(d);
                vo.setSubmitCnt(0);
                vo.setAcceptCnt(0);
                vo.setSolvedCnt(0);
            }
            result.add(vo);
        }
        return result;
    }
}
