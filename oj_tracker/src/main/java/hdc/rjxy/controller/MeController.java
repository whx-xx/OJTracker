package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.mapper.UserMapper;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.dto.UpdateMyPlatformsReq;
import hdc.rjxy.pojo.dto.UpdateNicknameReq;
import hdc.rjxy.pojo.dto.UpdateUsernameReq;
import hdc.rjxy.pojo.vo.MyPlatformAccountVO;
import hdc.rjxy.service.MyPlatformService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static hdc.rjxy.controller.AuthController.LOGIN_USER;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final MyPlatformService myPlatformService;
    private final UserMapper userMapper;

    public MeController(MyPlatformService myPlatformService, UserMapper userMapper) {
        this.myPlatformService = myPlatformService;
        this.userMapper = userMapper;
    }

    // 1. 获取当前登录用户信息
    @GetMapping
    public R<UserSession> me(HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        return R.ok(me);
    }

    // 2. 更新个人昵称
    @PostMapping("/nickname")
    public R<Void> updateNickname(@RequestBody UpdateNicknameReq req, HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");

        if (req.getNickname() == null || req.getNickname().isBlank()) {
            return R.fail(400, "昵称不能为空");
        }

        // 更新数据库
        userMapper.updateNickname(me.getId(), req.getNickname());

        // 关键：同步更新 Session，否则导航栏昵称不会立即改变
        me.setNickname(req.getNickname());
        session.setAttribute(LOGIN_USER, me);

        return R.ok(null);
    }

    // 3. 获取我绑定的平台账号列表
    @GetMapping("/platforms")
    public R<List<MyPlatformAccountVO>> listMine(HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        return R.ok(myPlatformService.listMine(me.getId()));
    }

    // 4. 绑定或更新平台账号 (Codeforces 等)
    @PostMapping("/platforms")
    public R<Void> updateMine(@RequestBody UpdateMyPlatformsReq req, HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        myPlatformService.updateMine(me.getId(), req);
        return R.ok(null);
    }

    // 5. 更新用户名
    @PostMapping("/username")
    public R<Void> updateUsername(@RequestBody UpdateUsernameReq req, HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");

        if (req.getUsername() == null || req.getUsername().isBlank()) {
            return R.fail(400, "用户名不能为空");
        }

        // 1. 更新数据库
        userMapper.updateUsername(me.getId(), req.getUsername());

        // 2. 同步更新 Session
        me.setUsername(req.getUsername());
        session.setAttribute(LOGIN_USER, me);

        return R.ok(null);
    }
}