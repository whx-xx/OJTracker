package hdc.rjxy.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
Root 容器：扫 service/init + 引入 MyBatisConfig
 */
@Configuration
@EnableScheduling
@ComponentScan(basePackages = {
        "hdc.rjxy.service",
        "hdc.rjxy.init",
        "hdc.rjxy.task",
})
@Import({
        MyBatisConfig.class,
        CfConfig.class,
        JacksonConfig.class,
        RedisConfig.class
})
public class RootConfig {
}
