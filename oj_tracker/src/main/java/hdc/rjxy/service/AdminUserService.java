package hdc.rjxy.service;

import hdc.rjxy.common.PageResult;
import hdc.rjxy.mapper.UserMapper;
import hdc.rjxy.pojo.User;
import hdc.rjxy.pojo.vo.AdminUserStatsVO;
import hdc.rjxy.pojo.vo.UserAdminVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {

    private final UserMapper userMapper;

    public AdminUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public PageResult<UserAdminVO> pageUsers(int page, int pageSize, String keyword) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;

        int offset = (page - 1) * pageSize;

        Long total = userMapper.countAdminList(keyword);
        List<UserAdminVO> list = userMapper.pageAdminList(keyword, offset, pageSize);
        return PageResult.of(total == null ? 0 : total, list);
    }

    @Transactional
    public void updateNickname(Long id, String nickname) {
        if (nickname == null || nickname.isBlank()) throw new IllegalArgumentException("昵称不能为空");
        User u = userMapper.findById(id);
        if (u == null) throw new IllegalArgumentException("用户不存在");
        userMapper.updateNickname(id, nickname.trim());
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) throw new IllegalArgumentException("status 只能是0或1");
        User u = userMapper.findById(id);
        if (u == null) throw new IllegalArgumentException("用户不存在");
        userMapper.updateStatus(id, status);
    }

    public AdminUserStatsVO getUserStats() {
        return userMapper.getAdminUserStats();
    }
}

