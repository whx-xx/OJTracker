package hdc.rjxy.service;

import hdc.rjxy.mapper.AdminOpLogMapper;
import hdc.rjxy.mapper.TeamMapper;
import hdc.rjxy.mapper.TeamMemberMapper;
import hdc.rjxy.mapper.UserMapper;
import hdc.rjxy.pojo.User;
import hdc.rjxy.pojo.UserSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final AdminOpLogMapper adminOpLogMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;

    public AuthService(UserMapper userMapper, AdminOpLogMapper adminOpLogMapper, TeamMapper teamMapper, TeamMemberMapper teamMemberMapper) {
        this.userMapper = userMapper;
        this.adminOpLogMapper = adminOpLogMapper;
        this.teamMapper = teamMapper;
        this.teamMemberMapper = teamMemberMapper;
    }

    /** username 可以是 admin，也可以是学号：优先按 username 查，查不到再按 studentNo 查 */
    public UserSession login(String usernameOrStudentNo, String password) {
        User user = userMapper.findByUsername(usernameOrStudentNo);
        if (user == null) {
            user = userMapper.findByStudentNo(usernameOrStudentNo);
        }
        if (user == null) return null;

        if (user.getStatus() != null && user.getStatus() == 0) {
            // 禁用用户不给登录
            throw new IllegalStateException("账号已被禁用，请联系管理员");
        }

        if (!encoder.matches(password, user.getPasswordHash())) {
            return null;
        }

        UserSession s = new UserSession();
        s.setId(user.getId());
        s.setUsername(user.getUsername());
        s.setStudentNo(user.getStudentNo());
        s.setNickname(user.getNickname());
        s.setRole(user.getRole());
        s.setStatus(user.getStatus());
        s.setMustChangePassword(user.getMustChangePassword());
        return s;
    }

    @Transactional
    public Long register(String studentNo, String username, String rawPassword) {
        if (studentNo == null || studentNo.isBlank()) {
            throw new IllegalArgumentException("学号不能为空");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("密码至少6位");
        }
        if (userMapper.findByStudentNo(studentNo) != null) {
            throw new IllegalArgumentException("学号已注册");
        }

        User u = new User();
        u.setStudentNo(studentNo);
        u.setUsername(username);
        u.setNickname(username);
        u.setRole("USER");
        u.setStatus(1);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setMustChangePassword(0);

        userMapper.insertUser(u);
        Long teamId = teamMapper.findIdByCode("DEFAULT");
        teamMemberMapper.addMember(teamId, u.getId());

        return u.getId();
    }

    @Transactional
    public void changePassword(Long userId, String oldPwd, String newPwd) {
        User user = userMapper.findById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalStateException("账号已被禁用");
        }

        if (!encoder.matches(oldPwd, user.getPasswordHash())) {
            throw new IllegalArgumentException("原密码错误");
        }
        if (newPwd == null || newPwd.length() < 6) {
            throw new IllegalArgumentException("新密码至少6位");
        }

        userMapper.updatePasswordAndMustChange(userId, encoder.encode(newPwd), 0);
    }

    @Transactional
    public void adminResetPassword(Long adminId, Long targetUserId, HttpServletRequest req) {
        // 重置为 000000 + 强制改密
        userMapper.updatePasswordAndMustChange(targetUserId, encoder.encode("000000"), 1);

        String ip = req.getRemoteAddr();
        adminOpLogMapper.insert(adminId, targetUserId, "RESET_PWD", ip, "reset to 000000");
    }
}
