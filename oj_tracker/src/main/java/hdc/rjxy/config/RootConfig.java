package hdc.rjxy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
// import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {
        "hdc.rjxy.service", // 扫描 Service 包
        "hdc.rjxy.init"     // 扫描 init 包
})
@Import({
    MyBatisConfig.class, // 数据库配置
    RedisConfig.class    // Redis配置
 })
public class RootConfig {
    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}