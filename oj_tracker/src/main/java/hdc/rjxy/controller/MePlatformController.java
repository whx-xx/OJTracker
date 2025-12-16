package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.dto.UpdateMyPlatformsReq;
import hdc.rjxy.pojo.vo.MyPlatformAccountVO;
import hdc.rjxy.service.MyPlatformService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static hdc.rjxy.controller.AuthController.LOGIN_USER;

@RestController
@RequestMapping("/api/me")
public class MePlatformController {

    private final MyPlatformService myPlatformService;

    public MePlatformController(MyPlatformService myPlatformService) {
        this.myPlatformService = myPlatformService;
    }

    @GetMapping("/platforms")
    public R<List<MyPlatformAccountVO>> listMine(HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        return R.ok(myPlatformService.listMine(me.getId()));
    }

    @PutMapping("/platforms")
    public R<Void> updateMine(@RequestBody UpdateMyPlatformsReq req, HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        myPlatformService.updateMine(me.getId(), req);
        return R.ok(null);
    }
}
