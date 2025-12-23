package hdc.rjxy.config;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import jakarta.servlet.Filter;

public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        // Root: 数据源/事务/MyBatis/Service/初始化器
        return new Class<?>[]{RootConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        // Web: Spring MVC
        return new Class<?>[]{WebMvcConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter f = new CharacterEncodingFilter();
        f.setEncoding("UTF-8");
        f.setForceEncoding(true);
        return new Filter[]{f};
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // 设置临时上传路径
        String location = System.getProperty("java.io.tmpdir");

        // maxFileSize: 单个文件最大大小 (例如 5MB)
        // maxRequestSize: 整个请求最大大小 (例如 10MB)
        // fileSizeThreshold: 文件大小阈值，超过后写入磁盘
        long maxFileSize = 5 * 1024 * 1024;
        long maxRequestSize = 10 * 1024 * 1024;
        int fileSizeThreshold = 0;

        MultipartConfigElement multipartConfigElement =
                new MultipartConfigElement(location, maxFileSize, maxRequestSize, fileSizeThreshold);

        registration.setMultipartConfig(multipartConfigElement);
    }
}
