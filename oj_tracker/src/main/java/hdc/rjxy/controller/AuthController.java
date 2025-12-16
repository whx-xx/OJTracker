package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.dto.ChangePasswordReq;
import hdc.rjxy.pojo.dto.LoginReq;
import hdc.rjxy.pojo.dto.RegisterReq;
import hdc.rjxy.service.AuthService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String LOGIN_USER = "LOGIN_USER";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<UserSession> login(@RequestBody LoginReq req, HttpSession session) {
        if (req.getUsername() == null || req.getPassword() == null) {
            return R.fail(400, "参数不能为空");
        }

        UserSession u = authService.login(req.getUsername(), req.getPassword());
        if (u == null) return R.fail(400, "账号或密码错误");

        session.setAttribute(LOGIN_USER, u);
        return R.ok(u);
    }

    @PostMapping("/register")
    public R<Long> register(@RequestBody RegisterReq req) {
        Long uid = authService.register(req.getStudentNo(), req.getUsername(), req.getPassword());
        return R.ok(uid);
    }

    @PostMapping("/change-password")
    public R<Void> changePassword(@RequestBody ChangePasswordReq req, HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");

        authService.changePassword(me.getId(), req.getOldPassword(), req.getNewPassword());

        // 改密后刷新 session（mustChangePassword 置 0）
        me.setMustChangePassword(0);
        session.setAttribute(LOGIN_USER, me);

        return R.ok(null);
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpSession session) {
        session.invalidate();
        return R.ok(null);
    }
}
