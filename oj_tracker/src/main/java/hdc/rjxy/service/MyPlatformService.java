package hdc.rjxy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.mapper.UserPlatformAccountMapper;
import hdc.rjxy.pojo.Platform;
import hdc.rjxy.pojo.UserPlatformAccount;
import hdc.rjxy.pojo.dto.UpdateMyPlatformsReq;
import hdc.rjxy.pojo.vo.MyPlatformAccountVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MyPlatformService {

    @Autowired
    private UserPlatformAccountMapper accountMapper;

    @Autowired
    private PlatformMapper platformMapper;

    public List<MyPlatformAccountVO> listMine(Long userId) {
        return accountMapper.listMine(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateMine(Long userId, UpdateMyPlatformsReq req) {
        if (req == null || req.getItems() == null) {
            throw new IllegalArgumentException("items不能为空");
        }

        for (UpdateMyPlatformsReq.Item item : req.getItems()) {
            if (item.getPlatformId() == null) continue;

            // 1. 校验平台是否存在且启用
            Platform p = platformMapper.selectById(item.getPlatformId());
            if (p == null || p.getEnabled() != 1) {
                throw new IllegalArgumentException("平台不存在或未启用: " + item.getPlatformId());
            }

            String type = item.getIdentifierType() == null ? "handle" : item.getIdentifierType().trim();
            if (type.isEmpty()) type = "handle";
            String val = item.getIdentifierValue() == null ? "" : item.getIdentifierValue().trim();

            // 构建查询条件：特定用户 + 特定平台
            LambdaQueryWrapper<UserPlatformAccount> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserPlatformAccount::getUserId, userId)
                    .eq(UserPlatformAccount::getPlatformId, item.getPlatformId());

            if (val.isBlank()) {
                // 2. 如果值为空，则解绑（删除记录）
                accountMapper.delete(wrapper);
            } else {
                // 3. 否则插入或更新
                UserPlatformAccount account = accountMapper.selectOne(wrapper);
                if (account == null) {
                    account = new UserPlatformAccount();
                    account.setUserId(userId);
                    account.setPlatformId(item.getPlatformId());
                    account.setIdentifierType(type);
                    account.setIdentifierValue(val);
                    account.setVerified(0);
                    account.setUpdatedAt(LocalDateTime.now());
                    accountMapper.insert(account);
                } else {
                    // 仅当值变化时更新
                    if (!val.equals(account.getIdentifierValue())) {
                        account.setIdentifierType(type);
                        account.setIdentifierValue(val);
                        account.setVerified(0); // 重新绑定后需要重新验证（如果业务需要）
                        account.setUpdatedAt(LocalDateTime.now());
                        // 由于没有单一主键，使用 wrapper 更新
                        accountMapper.update(account, wrapper);
                    }
                }
            }
        }
    }
}