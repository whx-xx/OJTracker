package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

import static hdc.rjxy.controller.AuthController.LOGIN_USER;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public R<UserSession> me(HttpSession session) {
        UserSession me = (UserSession) session.getAttribute(LOGIN_USER);
        if (me == null) return R.fail(401, "未登录");
        return R.ok(me);
    }
}
