package hdc.rjxy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import hdc.rjxy.pojo.User;
import hdc.rjxy.pojo.UserSession;
import hdc.rjxy.pojo.dto.LoginReq;
import hdc.rjxy.pojo.dto.RegisterReq;

public interface UserService extends IService<User> {
    /**
     * 登录逻辑
     * @return 登录成功后的 Session 对象
     */
    UserSession login(LoginReq req);

    /**
     * 注册新用户
     * @return 新用户的 ID
     */
    Long register(RegisterReq req);

    /**
     * 修改密码
     */
    void changePassword(Long userId, String oldPwd, String newPwd, String confirmPwd);
}