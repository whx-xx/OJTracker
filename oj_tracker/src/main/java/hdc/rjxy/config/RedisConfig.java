package hdc.rjxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@PropertySource("classpath:db.properties") // 加载配置文件
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // Session 30分钟失效 (1800秒)
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${redis.host}") String host,
            @Value("${redis.port}") int port,
            @Value("${redis.password}") String password) {

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isEmpty()) {
            config.setPassword(password);
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION"); // Cookie 名称
        serializer.setCookiePath("/");       // Cookie 路径

        // 关键设置：设置 Cookie 的最大生命周期（秒）
        // 例如：7 天 = 60 * 60 * 24 * 7 = 604800 秒
        // 如果设置为 -1，就是默认的“会话级 Cookie”（关闭浏览器即失效）
        serializer.setCookieMaxAge(604800);
        // 持久化 Cookie
        /*
        浏览器会把这个 Cookie 写到硬盘里。即使你关机重启，
        只要没过有效期，下次打开浏览器 Cookie 依然会发送给服务器。
        */
        return serializer;
    }
}