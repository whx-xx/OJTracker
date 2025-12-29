package hdc.rjxy.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:db.properties")
@MapperScan("hdc.rjxy.mapper") // 扫描 Mapper 接口
public class MyBatisConfig {

    // 1. 数据源配置 (HikariCP)
    @Bean
    public DataSource dataSource(Environment env) {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName(env.getProperty("jdbc.driverClassName"));
        ds.setJdbcUrl(env.getProperty("jdbc.url"));
        ds.setUsername(env.getProperty("jdbc.username"));
        ds.setPassword(env.getProperty("jdbc.password"));
        ds.setMaximumPoolSize(Integer.parseInt(env.getProperty("jdbc.pool.maxSize", "10")));
        return ds;
    }

    // 2. MyBatis-Plus 核心工厂 (关键点：使用 MybatisSqlSessionFactoryBean)
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        // 配置 MP 全局策略 (例如驼峰映射默认已开启)
        // 可以在这里配置 GlobalConfig，如主键策略等

        // 配置分页插件
        factory.setPlugins(mybatisPlusInterceptor());

        // 加载 XML Mapper 文件 (如果还需要手写 SQL)
        factory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml")
        );

        return factory.getObject();
    }

    // 3. MP 插件配置 (分页拦截器)
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加 MySQL 分页拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    // 4. 事务管理器
    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}