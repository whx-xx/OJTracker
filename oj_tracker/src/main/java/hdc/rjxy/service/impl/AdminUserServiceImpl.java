package hdc.rjxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.mapper.UserMapper;
import hdc.rjxy.pojo.User;
import hdc.rjxy.pojo.vo.UserAdminVO;
import hdc.rjxy.service.AdminUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Page<UserAdminVO> pageUsers(int pageNum, int pageSize, String keyword, String status) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getStudentNo, keyword));
        }
        if (Objects.equals(status, "0") || Objects.equals(status, "1")) {
             wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getId);

        userMapper.selectPage(page, wrapper);

        Page<UserAdminVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        List<UserAdminVO> vos = page.getRecords().stream().map(u -> {
            UserAdminVO vo = new UserAdminVO();
            BeanUtils.copyProperties(u, vo);
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        if  (status != 0 && status != 1) {
            throw new IllegalArgumentException("状态值错误");
        }

        // 简单保护：不允许禁用 ADMIN 角色（防止误操作自己）
        if ("ADMIN".equals(user.getRole()) && status == 0) {
            throw new IllegalArgumentException("不能禁用管理员账号");
        }

        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        user.setPasswordHash(passwordEncoder.encode("000000"));
        user.setMustChangePassword(1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void updateUserNickname(Long userId, String nickname) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        user.setNickname(nickname);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }
}