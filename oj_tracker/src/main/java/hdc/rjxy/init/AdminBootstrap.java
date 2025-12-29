package hdc.rjxy.init;


import hdc.rjxy.mapper.TeamMapper;
import hdc.rjxy.mapper.TeamMemberMapper;
import hdc.rjxy.mapper.UserMapper;
import hdc.rjxy.pojo.User;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AdminBootstrap {

    private final UserMapper userMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminBootstrap(UserMapper userMapper, TeamMemberMapper teamMemberMapper) {
        this.userMapper = userMapper;
        this.teamMemberMapper = teamMemberMapper;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initAdmin(ContextRefreshedEvent event) {
        // 只在 Root 容器执行一次，避免 MVC 子容器重复触发
        if (event.getApplicationContext().getParent() != null) return;

        User admin = userMapper.findByUsername("admin");
        if (admin != null) return;

        User u = new User();
        u.setUsername("admin");
        u.setNickname("管理员");
        u.setRole("ADMIN");
        u.setStatus(1);
        u.setPasswordHash(encoder.encode("000000"));
        u.setMustChangePassword(1);

        userMapper.insertAdmin(u);
        teamMemberMapper.addMember(1L, u.getId());
    }
}
/*
Spring MVC 有两个容器，
初始化代码只该跑在 Root 容器，
子容器一律不准碰初始化逻辑
*/