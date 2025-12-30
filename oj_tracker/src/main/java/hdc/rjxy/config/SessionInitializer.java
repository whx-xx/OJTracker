package hdc.rjxy.config;

import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

/**
 * 作用：自动注册 springSessionRepositoryFilter 过滤器
 * 确保所有请求的 HttpSession 被 Spring Session 接管
 */
public class SessionInitializer extends AbstractHttpSessionApplicationInitializer {
    // 不需要写任何代码，继承父类即可
}
/*
在 Spring Boot 中，框架会自动配置过滤器。但在纯 Spring MVC 中，
你必须创建一个初始化器来告诉 Servlet 容器：“请把所有请求先拦截下来，
把 HttpSession 偷梁换柱成 RedisSession”。
*/