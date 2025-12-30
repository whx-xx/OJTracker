package hdc.rjxy.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hdc.rjxy.pojo.User;
import hdc.rjxy.service.UserService;
import lombok.extern.slf4j.Slf4j; // 如果你有引入 Lombok Slf4j，或者使用 LoggerFactory
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdminBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrap.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 这一步是为了防止 Spring MVC 父子容器导致事件触发两次
    @EventListener(ContextRefreshedEvent.class)
    public void initAdmin(ContextRefreshedEvent event) {
        // 1. 只有 Root 容器（父容器）才执行，Spring MVC 的子容器跳过
        if (event.getApplicationContext().getParent() != null) {
            return;
        }

        // 2. 检查管理员是否存在
        // 使用 MyBatis-Plus 的 LambdaWrapper，类型安全
        long count = userService.count(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "ADMIN"));

        if (count > 0) {
            logger.info(">> 系统检测：管理员账号已存在，跳过初始化。");
            return;
        }

        logger.info(">> 系统检测：管理员账号不存在，正在创建默认管理员...");

        // 3. 创建管理员对象
        User admin = new User();
        admin.setStudentNo("20231819403010");
        admin.setUsername("admin");
        admin.setNickname("超级管理员");
        admin.setRole("ADMIN");
        admin.setStatus(1); // 启用
        admin.setMustChangePassword(1); // 强制改密
        admin.setPasswordHash(passwordEncoder.encode("000000")); // 默认密码

        // 设置默认头像
        admin.setAvatar("https://ui-avatars.com/api/?name=Admin&background=random");

        // 4. 保存
        userService.save(admin);

        logger.info(">> 管理员创建成功！账号: admin / 密码: 000000。请登录后立即修改密码。");

        // TODO: 等 TeamService 重构完成后，在此处恢复将管理员加入默认团队的逻辑
        // if (teamService != null) {
        //     teamService.joinDefaultTeam(admin.getId());
        // }
    }
}