package hdc.rjxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@PropertySource("classpath:db.properties") // 读取配置文件
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600) // 开启 Redis Session，过期时间 1 小时 (3600秒)
public class RedisConfig {

    @Value("${redis.host}")
    private String host;

    @Value("${redis.port}")
    private int port;

    // 容错处理：如果密码为空，给一个默认空字符串
    @Value("${redis.password:}")
    private String password;

    @Bean
    public LettuceConnectionFactory connectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isEmpty()) {
            config.setPassword(password);
        }
        return new LettuceConnectionFactory(config);
    }
    // 它是 Spring 应用与 Redis 服务器建立连接的“桥梁”和“连接管理器”
}