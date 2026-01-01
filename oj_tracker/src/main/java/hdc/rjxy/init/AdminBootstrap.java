package hdc.rjxy.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hdc.rjxy.mapper.PlatformMapper;
import hdc.rjxy.pojo.Platform;
import hdc.rjxy.pojo.User;
import hdc.rjxy.service.UserService;
import lombok.extern.slf4j.Slf4j;
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

    // 注入 PlatformMapper 用于操作平台表
    @Autowired
    private PlatformMapper platformMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 这一步是为了防止 Spring MVC 父子容器导致事件触发两次
    @EventListener(ContextRefreshedEvent.class)
    public void initAdmin(ContextRefreshedEvent event) {
        // 1. 只有 Root 容器（父容器）才执行，Spring MVC 的子容器跳过
        if (event.getApplicationContext().getParent() != null) {
            return;
        }

        // ==================== 1. 初始化管理员账号 ====================
        long count = userService.count(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "ADMIN"));

        if (count > 0) {
            logger.info(">> 系统检测：管理员账号已存在，跳过初始化。");
        } else {
            logger.info(">> 系统检测：管理员账号不存在，正在创建默认管理员...");

            // 创建管理员对象
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

            // 保存
            userService.save(admin);

            logger.info(">> 管理员创建成功！账号: admin / 密码: 000000。请登录后立即修改密码。");
        }

        // ==================== 2. [新增] 初始化 CF 平台 ====================
        // 使用 Mapper 的 selectCount 查询
        Long cfCount = platformMapper.selectCount(new LambdaQueryWrapper<Platform>()
                .eq(Platform::getCode, "CF"));

        if (cfCount > 0) {
            logger.info(">> 系统检测：Codeforces 平台已存在，跳过初始化。");
        } else {
            logger.info(">> 系统检测：Codeforces 平台不存在，正在初始化...");

            Platform cf = new Platform();
            cf.setCode("CF");
            cf.setName("Codeforces");
            cf.setEnabled(1); // 1 = 启用

            platformMapper.insert(cf);

            logger.info(">> Codeforces 平台初始化成功！");
        }
    }
}