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
import org.springframework.util.StringUtils;

import java.util.HashSet;
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
    public Page<AdminOpLogVO> page(int pageNum, int pageSize, String opType, String keyword) {
        // 1. 构造查询条件 Wrapper
        LambdaQueryWrapper<AdminOpLog> wrapper = new LambdaQueryWrapper<>();

        // A. 筛选操作类型
        if (StringUtils.hasText(opType)) {
            wrapper.eq(AdminOpLog::getOpType, opType);
        }

        // B. 筛选关键字 (处理目标对象的模糊搜索)
        if (StringUtils.hasText(keyword)) {
            // B1. 先去用户表查，看看有没有名字匹配 keyword 的用户，拿到他们的 ID
            List<User> matchedUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .like(User::getNickname, keyword)
                    .or().like(User::getStudentNo, keyword)
                    .select(User::getId)); // 性能优化：只查ID字段

            Set<Long> matchedIds = matchedUsers.stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());

            // B2. 构造组合查询：(备注包含关键字 OR 目标用户是这些人 OR 操作管理员是这些人)
            wrapper.and(w -> {
                w.like(AdminOpLog::getRemark, keyword); // 搜备注
                if (!matchedIds.isEmpty()) {
                    w.or().in(AdminOpLog::getTargetUserId, matchedIds) // 搜目标对象
                            .or().in(AdminOpLog::getAdminId, matchedIds);     // 搜操作人
                }
            });
        }

        // C. 排序：按时间倒序
        wrapper.orderByDesc(AdminOpLog::getOpTime);

        // 2. 执行分页查询
        Page<AdminOpLog> page = new Page<>(pageNum, pageSize);
        adminOpLogMapper.selectPage(page, wrapper);

        // 如果没查到数据，直接返回空页，避免后续无效查询
        if (page.getRecords().isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        // 3. 收集所有需要回显名字的 UserID (包含 adminId 和 targetUserId)
        Set<Long> userIds = new HashSet<>();
        page.getRecords().forEach(log -> {
            if (log.getAdminId() != null) userIds.add(log.getAdminId());
            if (log.getTargetUserId() != null) userIds.add(log.getTargetUserId());
        });

        // 4. 批量查询用户信息 (ID -> Nickname 映射)
        Map<Long, String> userMap;
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            // 优先显示昵称，没有昵称显示用户名
            userMap = users.stream().collect(Collectors.toMap(
                    User::getId,
                    u -> StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername()
            ));
        } else {
            userMap = Map.of();
        }

        // 5. 组装 VO 返回给前端
        List<AdminOpLogVO> vos = page.getRecords().stream().map(log -> {
            AdminOpLogVO vo = new AdminOpLogVO();
            BeanUtils.copyProperties(log, vo);

            // 填充名字，如果没有查到显示 Unknown
            vo.setAdminName(userMap.getOrDefault(log.getAdminId(), "未知管理员(ID:" + log.getAdminId() + ")"));

            // 只有当有目标对象时才填充名字，否则由前端显示“全局”
            if (log.getTargetUserId() != null) {
                vo.setTargetUserName(userMap.getOrDefault(log.getTargetUserId(), "未知用户"));
            }

            return vo;
        }).collect(Collectors.toList());

        // 构造返回的分页对象
        Page<AdminOpLogVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }
}