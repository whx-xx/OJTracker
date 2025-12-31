package hdc.rjxy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.pojo.vo.UserAdminVO;

public interface AdminUserService {
    Page<UserAdminVO> pageUsers(int page, int size, String keyword, String status);
    void updateUserStatus(Long userId, Integer status);
    void resetPassword(Long userId);
    void updateUserNickname(Long userId, String nickname);
}