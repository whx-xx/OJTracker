package hdc.rjxy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
@EnableWebMvc // 开启 Spring MVC 支持
@ComponentScan(basePackages = {
        "hdc.rjxy.controller", // 扫描 Controller
        "hdc.rjxy.common",     // 扫描公共组件
        "org.springdoc"        // 扫描 SpringDoc 的 Controller 和配置类
})
@Import({
        SpringDocConfig.class // 你自己的 Swagger 文档信息配置
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
                        // Swagger 放行
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",  // 接口数据 JSON
                        "/webjars/**"
                );
    }


    // --- JSON 转换器配置 (支持 Java 8 时间类型) ---
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 1. [关键修复] 添加字节数组转换器
        // 必须加在 Jackson 之前，否则 SpringDoc 返回的 byte[] 会被 Jackson 转成 Base64
        converters.add(new ByteArrayHttpMessageConverter());

        // 2. 添加字符串转换器 (防止返回 String 时乱码或被转义)
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));

        ObjectMapper mapper = new ObjectMapper();
        // 注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());
        // 禁止将日期写为时间戳，而是 ISO 字符串
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converters.add(new MappingJackson2HttpMessageConverter(mapper));
    }

    // --- Thymeleaf 模板解析器 ---
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix("/WEB-INF/templates/"); // 模板文件位置
        resolver.setSuffix(".html");               // 后缀
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

    // --- 静态资源映射 (CSS/JS/图片) ---
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 URL 中的 /static/** 映射到工程目录的 /static/ 下
        registry.addResourceHandler("/static/**")
                .addResourceLocations("/static/");

        // Swagger UI 资源映射
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/5.17.14/");

        // 映射 webjars (Swagger 依赖的基础资源)
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(false); // 开发环境建议关闭缓存
    }
}