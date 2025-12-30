package hdc.rjxy.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI ojTrackerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OJ Tracker API 文档")
                        .description("基于 Spring MVC 6 的 OJ 数据统计系统接口文档")
                        .version("v2.0")
                        .contact(new Contact()
                                .name("whx")
                                .email("3243916556@qq.com")));
    }
}