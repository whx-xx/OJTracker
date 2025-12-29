package hdc.rjxy.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {
        "hdc.rjxy.service" // 扫描 Service 层
})
// @Import({
//     MyBatisConfig.class, // 暂时注释：数据库配置
//     RedisConfig.class    // 暂时注释：Redis配置
// })
public class RootConfig {
    // 目前是个空配置，仅作为 Root 容器的标识
}