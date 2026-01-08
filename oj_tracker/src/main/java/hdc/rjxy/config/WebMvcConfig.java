package hdc.rjxy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hdc.rjxy.common.ForcePasswordChangeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebMvc               // 开启 Spring MVC 支持
@EnableAspectJAutoProxy     // 开启 AOP 代理
@ComponentScan(
        basePackages = {
                "hdc.rjxy.controller", // 扫描 Controller
                "hdc.rjxy.common",     // 扫描公共组件
                "org.springdoc",       // 扫描 SpringDoc
                "hdc.rjxy.aop"
        },
        // 排除 SpringDoc 自带的 SwaggerUiHome，解决根路径 "/" 冲突问题
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.springdoc\\.webmvc\\.ui\\.SwaggerUiHome")
)
@Import({
        SpringDocConfig.class // Swagger 文档配置
})
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private ApplicationContext applicationContext;

    // 拦截器配置
    @Autowired
    private hdc.rjxy.common.AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",
                        "/login",
                        "/register",
                        "/api/auth/**",
                        "/static/**",
                        "/favicon.ico",
                        "/css/**",
                        "/js/**",
                        "/error",
                        // Swagger 放行
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/webjars/**"
                );
        // 注册强制修改密码拦截器
        registry.addInterceptor(new ForcePasswordChangeInterceptor())
                .addPathPatterns("/**"); // 拦截所有路径，具体的白名单逻辑在拦截器内部处理
    }

    // --- JSON 转换器配置 ---
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 1. 添加字节数组转换器 (防止 swagger 图片乱码)
        converters.add(new ByteArrayHttpMessageConverter());

        // 2. 添加字符串转换器
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));

        // 3. Jackson 配置
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converters.add(new MappingJackson2HttpMessageConverter(mapper));
    }

    // --- Thymeleaf 模板解析器 ---
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix("/WEB-INF/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false); // 开发环境关闭缓存
        return resolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templateResolver());
        return engine;
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(templateEngine());
        resolver.setCharacterEncoding("UTF-8");
        registry.viewResolver(resolver);
    }

    // --- 静态资源映射 ---

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 将根路径的 /favicon.ico 请求，直接“转发”给静态资源路径
        // 这样浏览器访问 /favicon.ico 时，服务器内部会自动去拿 /static/images/favicon.ico
        registry.addViewController("/favicon.ico")
                .setViewName("forward:/static/images/favicon.ico");
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源 (CSS/JS/Img)
        registry.addResourceHandler("/static/**")
                .addResourceLocations("/static/");

        // Swagger UI
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/5.17.14/"); // 注意版本号可能需要根据实际jar包调整，或者直接指向 swagger-ui 目录

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(false);
    }
}