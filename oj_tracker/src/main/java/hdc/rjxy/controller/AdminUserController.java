package hdc.rjxy.controller;

import hdc.rjxy.common.PageResult;
import hdc.rjxy.common.R;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.dto.UpdateNicknameReq;
import hdc.rjxy.pojo.dto.UpdateStatusReq;
import hdc.rjxy.pojo.vo.AdminUserStatsVO;
import hdc.rjxy.pojo.vo.UserAdminVO;
import hdc.rjxy.service.AdminUserService;
import hdc.rjxy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import static hdc.rjxy.controller.AuthController.LOGIN_USER;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AuthService authService;
    public AdminUserController(AdminUserService adminUserService, AuthService authService) {
        this.adminUserService = adminUserService;
        this.authService = authService;
    }


    @GetMapping
    public R<PageResult<UserAdminVO>> page(@RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                           @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize,
                                           @RequestParam(value = "keyword", required = false) String keyword) {
        return R.ok(adminUserService.pageUsers(page, pageSize, keyword));
    }

    @PutMapping("/{id}/nickname")
    public R<Void> updateNickname(@PathVariable("id") Long id,
                                  @RequestBody UpdateNicknameReq req) {
        adminUserService.updateNickname(id, req.getNickname());
        return R.ok(null);
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable("id") Long id,
                                @RequestBody UpdateStatusReq req) {
        adminUserService.updateStatus(id, req.getStatus());
        return R.ok(null);
    }

    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable("id") Long targetUserId,
                                       HttpSession session,
                                       HttpServletRequest req) {
        UserSession admin = (UserSession) session.getAttribute(LOGIN_USER);
        if (admin == null) return R.fail(401, "未登录");
        if (!"ADMIN".equals(admin.getRole())) return R.fail(403, "无权限");

        authService.adminResetPassword(admin.getId(), targetUserId, req);
        return R.ok(null);
    }

    @GetMapping("/stats")
    public R<AdminUserStatsVO> getStats() {
        return R.ok(adminUserService.getUserStats());
    }

}
