package hdc.rjxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.UserPlatformAccountMapper;
import hdc.rjxy.pojo.Platform;
import hdc.rjxy.pojo.UserPlatformAccount;
import hdc.rjxy.pojo.dto.UpdateMyPlatformsReq;
import hdc.rjxy.pojo.vo.MyPlatformAccountVO;
import hdc.rjxy.service.MyPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MyPlatformServiceImpl extends ServiceImpl<UserPlatformAccountMapper, UserPlatformAccount> implements MyPlatformService {

    @Autowired
    private PlatformMapper platformMapper;

    @Override
    public List<MyPlatformAccountVO> listMine(Long userId) {
        return baseMapper.listMine(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMine(Long userId, UpdateMyPlatformsReq req) {
        if (req == null || req.getItems() == null) {
            throw new IllegalArgumentException("items不能为空");
        }

        for (UpdateMyPlatformsReq.Item item : req.getItems()) {
            if (item.getPlatformId() == null) continue;

            Platform p = platformMapper.selectById(item.getPlatformId());
            if (p == null || p.getEnabled() != 1) {
                throw new IllegalArgumentException("平台不存在或未启用: " + item.getPlatformId());
            }

            String type = item.getIdentifierType() == null ? "handle" : item.getIdentifierType().trim();
            if (type.isEmpty()) type = "handle";
            String val = item.getIdentifierValue() == null ? "" : item.getIdentifierValue().trim();

            LambdaQueryWrapper<UserPlatformAccount> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserPlatformAccount::getUserId, userId)
                    .eq(UserPlatformAccount::getPlatformId, item.getPlatformId());

            if (val.isBlank()) {
                this.remove(wrapper); // 使用 MP 的 remove
            } else {
                UserPlatformAccount account = this.getOne(wrapper); // 使用 MP 的 getOne
                if (account == null) {
                    account = new UserPlatformAccount();
                    account.setUserId(userId);
                    account.setPlatformId(item.getPlatformId());
                    account.setIdentifierType(type);
                    account.setIdentifierValue(val);
                    account.setVerified(0);
                    account.setUpdatedAt(LocalDateTime.now());
                    this.save(account); // 使用 MP 的 save
                } else {
                    if (!val.equals(account.getIdentifierValue())) {
                        account.setIdentifierType(type);
                        account.setIdentifierValue(val);
                        account.setVerified(0);
                        account.setUpdatedAt(LocalDateTime.now());
                        this.update(account, wrapper); // 使用 MP 的 update
                    }
                }
            }
        }
    }
}