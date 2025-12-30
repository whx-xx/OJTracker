package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.dto.ChangePasswordReq;
import hdc.rjxy.pojo.dto.LoginReq;
import hdc.rjxy.pojo.dto.RegisterReq;
import hdc.rjxy.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public R<UserSession> login(@RequestBody LoginReq req, HttpSession session) {
        // 1. 调用 Service 登录
        UserSession userSession = userService.login(req);

        // 2. 存入 HttpSession
        session.setAttribute("user", userSession);

        // 3. 返回给前端
        return R.ok(userSession);
    }

    @PostMapping("/register")
    public R<Long> register(@RequestBody RegisterReq req) {
        Long uid = userService.register(req);
        return R.ok(uid);
    }

    // 修改密码
    @PostMapping("/change-password")
    public R<String> changePassword(@RequestBody ChangePasswordReq req, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user");
        if (user == null) {
            return R.fail(401, "未登录");
        }

        userService.changePassword(user.getId(), req.getOldPassword(), req.getNewPassword(), req.getConfirmPassword());

        // 登出，让用户重新登录
        session.invalidate();
        return R.ok("密码修改成功，请重新登录");
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpSession session) {
        session.invalidate(); // 销毁 Session
        return R.ok();
    }

    @GetMapping("/current")
    public R<UserSession> currentUser(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user");
        if (user == null) {
            return R.fail(401, "未登录");
        }
        return R.ok(user);
    }
}