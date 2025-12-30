package hdc.rjxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import hdc.rjxy.mapper.UserMapper;
import hdc.rjxy.pojo.User;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.dto.LoginReq;
import hdc.rjxy.pojo.dto.RegisterReq;
import hdc.rjxy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserSession login(LoginReq req) {
        // 1. 校验参数
        Assert.hasText(req.getUsername(), "用户名不能为空");
        Assert.hasText(req.getPassword(), "密码不能为空");

        // 2. 用户名或学号登录
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername())
                .or()
                .eq(User::getStudentNo, req.getUsername())); // 允许输入学号作为用户名

        // 3. 校验用户是否存在
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 4. 校验密码 (BCrypt)
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("密码错误");
        }

        // 5. 校验状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用");
        }

        // 6. 返回 Session 对象
        return toSession(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterReq req) {
        Assert.hasText(req.getStudentNo(), "学号不能为空");
        Assert.hasText(req.getUsername(), "用户名不能为空");

        // 学号检验逻辑
        if (!req.getStudentNo().matches("^20\\\\d{12}$")) {
            throw new IllegalArgumentException("学号格式错误：请输入14位学号（如 20231819403010）");
        }

        // 密码至少六位
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw new IllegalArgumentException("密码至少6位");
        }

        // 检查学号是否已存在
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getStudentNo, req.getStudentNo()));
        if (count > 0) {
            throw new IllegalArgumentException("该学号已注册");
        }

        // 检查用户名是否重复
        long nameCount = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (nameCount > 0) {
            throw new IllegalArgumentException("用户名已被占用");
        }

        // 创建用户对象
        User u = new User();
        u.setStudentNo(req.getStudentNo());
        u.setUsername(req.getUsername());
        u.setNickname(req.getUsername()); // 默认昵称同用户名
        u.setRole("USER");
        u.setStatus(1); // 默认启用
        u.setMustChangePassword(0);
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));

        // 4. 保存到数据库
        this.save(u);

        return u.getId();
    }

    @Override
    public void changePassword(Long userId, String oldPwd, String newPwd, String confirmPwd) {
        User user = this.getById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        if (newPwd == null || newPwd.length() < 6) {
            throw new IllegalArgumentException("新密码至少6位");
        }

        // 确认密码
        if (!newPwd.equals(confirmPwd)) {
            throw new IllegalArgumentException("两次密码不一致");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPwd, user.getPasswordHash())) {
            throw new IllegalArgumentException("原密码错误");
        }

        // 验证新密码
        if (passwordEncoder.matches(newPwd, user.getPasswordHash())) {
            throw new IllegalArgumentException("新密码不能与原密码相同");
        }


        // 更新密码 & 清除强制改密标记
        user.setPasswordHash(passwordEncoder.encode(newPwd));
        user.setMustChangePassword(0);
        this.updateById(user);
    }

    private UserSession toSession(User user) {
        UserSession s = new UserSession();
        s.setId(user.getId());
        s.setUsername(user.getUsername());
        s.setStudentNo(user.getStudentNo());
        s.setNickname(user.getNickname());
        s.setRole(user.getRole());
        s.setStatus(user.getStatus());
        s.setMustChangePassword(user.getMustChangePassword());
        s.setAvatar(user.getAvatar());
        return s;
    }
}