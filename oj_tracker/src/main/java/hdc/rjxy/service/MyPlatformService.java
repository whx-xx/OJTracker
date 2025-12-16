package hdc.rjxy.service;

import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.UserPlatformAccountMapper;
import hdc.rjxy.pojo.Platform;
import hdc.rjxy.pojo.dto.UpdateMyPlatformsReq;
import hdc.rjxy.pojo.vo.MyPlatformAccountVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class MyPlatformService {

    private final UserPlatformAccountMapper accountMapper;
    private final PlatformMapper platformMapper;

    public MyPlatformService(UserPlatformAccountMapper accountMapper, PlatformMapper platformMapper) {
        this.accountMapper = accountMapper;
        this.platformMapper = platformMapper;
    }

    public List<MyPlatformAccountVO> listMine(Long userId) {
        return accountMapper.listMine(userId);
    }

    @Transactional
    public void updateMine(Long userId, UpdateMyPlatformsReq req) {
        if (req == null || req.getItems() == null) {
            throw new IllegalArgumentException("items不能为空");
        }

        for (UpdateMyPlatformsReq.Item it : req.getItems()) {
            if (it.getPlatformId() == null) throw new IllegalArgumentException("platformId不能为空");
            Platform p = platformMapper.findEnabledById(it.getPlatformId());
            if (p == null) throw new IllegalArgumentException("平台不存在或未启用: " + it.getPlatformId());

            String type = it.getIdentifierType() == null ? "" : it.getIdentifierType().trim();
            String val  = it.getIdentifierValue() == null ? "" : it.getIdentifierValue().trim();

            if (val.isBlank()) {
                accountMapper.deleteOne(userId, it.getPlatformId());
                continue;
            }

            if (type.isBlank()) type = "handle"; // 给默认值，第一版够用
            accountMapper.upsert(userId, it.getPlatformId(), type, val);
        }
    }
}
