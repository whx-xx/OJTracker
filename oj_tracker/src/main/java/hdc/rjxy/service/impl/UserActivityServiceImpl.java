package hdc.rjxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hdc.rjxy.mapper.DailyActivityMapper;
import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.UserPlatformAccountMapper;
import hdc.rjxy.pojo.DailyActivity;
import hdc.rjxy.pojo.Platform;
import hdc.rjxy.pojo.UserPlatformAccount;
import hdc.rjxy.pojo.vo.HeatmapDayVO;
import hdc.rjxy.service.UserActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserActivityServiceImpl implements UserActivityService {

    @Autowired
    private DailyActivityMapper dailyActivityMapper;
    @Autowired
    private PlatformMapper platformMapper;
    @Autowired
    private UserPlatformAccountMapper upaMapper;

    @Override
    public List<HeatmapDayVO> heatmap(Long userId, String platformCode, int days) {
        if (platformCode == null || platformCode.isBlank()) throw new IllegalArgumentException("platformCode不能为空");
        if (days <= 0 || days > 365) days = 90;

        Platform p = platformMapper.selectOne(new LambdaQueryWrapper<Platform>().eq(Platform::getCode, platformCode));
        if (p == null) throw new IllegalArgumentException("平台不存在: " + platformCode);

        UserPlatformAccount account = upaMapper.selectOne(new LambdaQueryWrapper<UserPlatformAccount>()
                .eq(UserPlatformAccount::getUserId, userId)
                .eq(UserPlatformAccount::getPlatformId, p.getId()));

        if (account == null || account.getIdentifierValue() == null) {
            return new ArrayList<>();
        }
        String handle = account.getIdentifierValue();

        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);
        LocalDate start = today.minusDays(days - 1L);

        List<DailyActivity> logs = dailyActivityMapper.selectList(new LambdaQueryWrapper<DailyActivity>()
                .eq(DailyActivity::getUserId, userId)
                .eq(DailyActivity::getPlatformId, p.getId())
                .eq(DailyActivity::getHandle, handle)
                .ge(DailyActivity::getDay, start)
                .le(DailyActivity::getDay, today)
                .orderByAsc(DailyActivity::getDay));

        Map<LocalDate, DailyActivity> map = new HashMap<>();
        for (DailyActivity log : logs) map.put(log.getDay(), log);

        List<HeatmapDayVO> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            DailyActivity log = map.get(d);
            HeatmapDayVO vo = new HeatmapDayVO();
            vo.setDay(d);
            if (log != null) {
                vo.setSubmitCnt(log.getSubmitCnt());
                vo.setAcceptCnt(log.getAcceptCnt());
                vo.setSolvedCnt(log.getSolvedCnt());
            } else {
                vo.setSubmitCnt(0);
                vo.setAcceptCnt(0);
                vo.setSolvedCnt(0);
            }
            result.add(vo);
        }
        return result;
    }
}