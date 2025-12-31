package hdc.rjxy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.aop.LogAdminOp;
import hdc.rjxy.common.R;
import hdc.rjxy.pojo.dto.ChangePasswordReq;
import hdc.rjxy.pojo.dto.UpdateNicknameReq;
import hdc.rjxy.pojo.dto.UpdateStatusReq;
import hdc.rjxy.pojo.vo.UserAdminVO;
import hdc.rjxy.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台-用户管理", description = "管理员管理用户信息")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "用户列表")
    @GetMapping
    public R<Page<UserAdminVO>> list(@RequestParam(value = "page", defaultValue = "1") int page,
                                     @RequestParam(value = "size", defaultValue = "20") int size,
                                     @RequestParam(value = "keyword", required = false) String keyword,
                                     @RequestParam(value = "status", required = false) String status) {
        return R.ok(adminUserService.pageUsers(page, size, keyword, status));
    }

    @LogAdminOp("更新用户状态")
    @Operation(summary = "更新状态", description = "封禁或解封用户")
    @PutMapping("/{userId}/status")
    public R<Void> updateStatus(@PathVariable(value = "userId") Long userId, @RequestBody UpdateStatusReq req) {
        adminUserService.updateUserStatus(userId, req.getStatus());
        return R.ok(null);
    }

    @LogAdminOp("重置用户密码")
    @Operation(summary = "重置密码")
    @PutMapping("/{userId}/reset-password")
    public R<Void> resetPassword(@PathVariable(value = "userId") Long userId) {
        adminUserService.resetPassword(userId);
        return R.ok(null);
    }

    @LogAdminOp("强制修改昵称")
    @Operation(summary = "修改昵称")
    @PutMapping("/{userId}/nickname")
    public R<Void> updateNickname(@PathVariable(value = "userId") Long userId, @RequestBody UpdateNicknameReq req) {
        // UpdateNicknameReq 在之前重构 MeController 时已经有了
        adminUserService.updateUserNickname(userId, req.getNickname());
        return R.ok(null);
    }
}