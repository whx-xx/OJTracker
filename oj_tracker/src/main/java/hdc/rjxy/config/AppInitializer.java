package hdc.rjxy.config;

import jakarta.servlet.Filter;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * 整个 Web 应用的启动入口类
 * 相当于 web.xml 的替代品
 */
public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    // 1. 加载根容器配置 (Service, DAO 层)
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{RootConfig.class};
    }

    // 2. 加载 Web 容器配置 (Controller, ViewResolver 层)
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{WebMvcConfig.class};
    }

    // 3. 配置 DispatcherServlet 拦截所有请求 ("/")
    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    // 4. 配置过滤器：强制使用 UTF-8 编码，防止中文乱码
    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return new Filter[]{encodingFilter};
    }
}