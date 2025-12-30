package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.dto.ChangePasswordReq;
import hdc.rjxy.pojo.dto.LoginReq;
import hdc.rjxy.pojo.dto.RegisterReq;
import hdc.rjxy.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证相关", description = "包含登录、注册、修改密码、注销等接口") // 类注解：定义模块名称
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Operation(summary = "用户登录", description = "支持用户名或学号登录，成功后返回 Session 信息") // 方法注解：接口描述
    @PostMapping("/login")
    public R<UserSession> login(@RequestBody LoginReq req, HttpSession session) {
        // 1. 调用 Service 登录
        UserSession userSession = userService.login(req);

        // 2. 存入 HttpSession
        session.setAttribute("user", userSession);

        // 3. 返回给前端
        return R.ok(userSession);
    }

    @Operation(summary = "用户注册", description = "注册新用户，学号必须为20开头的14位数字")
    @PostMapping("/register")
    public R<Long> register(@RequestBody RegisterReq req) {
        Long uid = userService.register(req);
        return R.ok(uid);
    }

    @Operation(summary = "修改密码", description = "登录状态下修改密码")
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

    @Operation(summary = "退出登录", description = "销毁当前 Session")
    @PostMapping("/logout")
    public R<Void> logout(HttpSession session) {
        session.invalidate(); // 销毁 Session
        return R.ok();
    }

    @Operation(summary = "获取当前用户", description = "获取当前登录用户的 Session 信息")
    @GetMapping("/current")
    public R<UserSession> currentUser(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user");
        if (user == null) {
            return R.fail(401, "未登录");
        }
        return R.ok(user);
    }
}