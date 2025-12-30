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
当你引入了 spring-session-data-redis 并配置了 SessionInitializer 后，
Spring Session 会自动注册一个优先级非常高的过滤器，叫做 SessionRepositoryFilter。
这个过滤器的作用就是拦截所有的 HTTP 请求，在请求到达你的 Controller 之前，
把它原本的 HttpServletRequest 和 HttpServletResponse 偷偷换掉
*/