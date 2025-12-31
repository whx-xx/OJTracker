package hdc.rjxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.mapper.AdminOpLogMapper;
import hdc.rjxy.mapper.UserMapper;
import hdc.rjxy.pojo.AdminOpLog;
import hdc.rjxy.pojo.User;
import hdc.rjxy.pojo.vo.AdminOpLogVO;
import hdc.rjxy.service.AdminOpLogService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminOpLogServiceImpl implements AdminOpLogService {

    @Autowired
    private AdminOpLogMapper adminOpLogMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void saveLog(AdminOpLog log) {
        adminOpLogMapper.insert(log);
    }

    @Override
    public Page<AdminOpLogVO> page(int pageNum, int pageSize) {
        // 1. 分页查日志
        Page<AdminOpLog> page = new Page<>(pageNum, pageSize);
        adminOpLogMapper.selectPage(page, new LambdaQueryWrapper<AdminOpLog>()
                .orderByDesc(AdminOpLog::getOpTime));

        // 2. 收集所有需要查询名字的 UserID (包含 adminId 和 targetUserId)
        Set<Long> userIds = page.getRecords().stream()
                .map(AdminOpLog::getAdminId)
                .collect(Collectors.toSet());
        userIds.addAll(page.getRecords().stream()
                .filter(l -> l.getTargetUserId() != null)
                .map(AdminOpLog::getTargetUserId)
                .collect(Collectors.toSet()));

        // 3. 批量查询用户信息
        Map<Long, String> userMap;
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, User::getNickname));
        } else {
            userMap = Map.of();
        }

        // 4. 组装 VO
        List<AdminOpLogVO> vos = page.getRecords().stream().map(log -> {
            AdminOpLogVO vo = new AdminOpLogVO();
            BeanUtils.copyProperties(log, vo);

            // 填充名字
            vo.setAdminName(userMap.getOrDefault(log.getAdminId(), "未知管理员"));
            if (log.getTargetUserId() != null) {
                vo.setTargetUserName(userMap.getOrDefault(log.getTargetUserId(), "未知用户"));
            }

            return vo;
        }).collect(Collectors.toList());

        Page<AdminOpLogVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }
}