package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.User;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.dto.UpdateMyPlatformsReq;
import hdc.rjxy.pojo.dto.UpdateNicknameReq;
import hdc.rjxy.pojo.dto.UpdateUsernameReq;
import hdc.rjxy.pojo.vo.MyPlatformAccountVO;
import hdc.rjxy.service.MyPlatformService;
import hdc.rjxy.service.OssService;
import hdc.rjxy.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "个人中心", description = "管理个人资料、绑定平台账号")
@RestController
@RequestMapping("/api/me")
public class MeController {

    @Autowired
    private MyPlatformService myPlatformService;

    @Autowired
    private UserService userService;

    @Autowired
    private OssService ossService;

    private UserSession getUserSession(HttpSession session) {
        return (UserSession) session.getAttribute("user");
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping
    public R<UserSession> me(HttpSession session) {
        UserSession me = getUserSession(session);
        if (me == null) return R.fail(401, "未登录");
        return R.ok(me);
    }

    @Operation(summary = "修改昵称")
    @PostMapping("/nickname")
    public R<Void> updateNickname(@RequestBody UpdateNicknameReq req, HttpSession session) {
        UserSession me = getUserSession(session);
        if (me == null) return R.fail(401, "未登录");

        if (req.getNickname() == null || req.getNickname().isBlank()) {
            return R.fail(400, "昵称不能为空");
        }

        User user = new User();
        user.setId(me.getId());
        user.setNickname(req.getNickname());
        user.setUpdatedAt(LocalDateTime.now());

        userService.updateById(user); // 使用 MyBatis-Plus Service 更新

        // 同步 Session
        me.setNickname(req.getNickname());
        session.setAttribute("user", me);

        return R.ok(null);
    }

    @Operation(summary = "修改用户名")
    @PostMapping("/username")
    public R<Void> updateUsername(@RequestBody UpdateUsernameReq req, HttpSession session) {
        UserSession me = getUserSession(session);
        if (me == null) return R.fail(401, "未登录");

        if (req.getUsername() == null || req.getUsername().isBlank()) {
            return R.fail(400, "用户名不能为空");
        }

        User user = new User();
        user.setId(me.getId());
        user.setUsername(req.getUsername());
        user.setUpdatedAt(LocalDateTime.now());

        userService.updateById(user);

        // 同步 Session
        me.setUsername(req.getUsername());
        session.setAttribute("user", me);

        return R.ok(null);
    }

    @Operation(summary = "获取绑定的平台列表")
    @GetMapping("/platforms")
    public R<List<MyPlatformAccountVO>> listMine(HttpSession session) {
        UserSession me = getUserSession(session);
        if (me == null) return R.fail(401, "未登录");
        return R.ok(myPlatformService.listMine(me.getId()));
    }

    @Operation(summary = "绑定/解绑平台账号", description = "value为空时代表解绑")
    @PostMapping("/platforms")
    public R<Void> updateMine(@RequestBody UpdateMyPlatformsReq req, HttpSession session) {
        UserSession me = getUserSession(session);
        if (me == null) return R.fail(401, "未登录");
        myPlatformService.updateMine(me.getId(), req);
        return R.ok(null);
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UserSession me = (session != null) ? (UserSession) session.getAttribute("user") : null;
        if (me == null) return R.fail(401, "未登录");

        if (file.isEmpty()) return R.fail(400, "文件不能为空");

        String avatarUrl = ossService.uploadFile(file);
        if (avatarUrl == null) return R.fail(500, "图片上传失败，请检查服务器配置");

        User user = new User();
        user.setId(me.getId());
        user.setAvatar(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());

        userService.updateById(user);

        // 同步 Session
        me.setAvatar(avatarUrl);
        session.setAttribute("user", me);

        return R.ok(avatarUrl);
    }
}