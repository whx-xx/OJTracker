package hdc.rjxy.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

@Service
public class OssService {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String urlPrefix;

    public OssService() {
        try {
            Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("oss.properties"));
            this.endpoint = props.getProperty("aliyun.oss.endpoint");
            this.accessKeyId = props.getProperty("aliyun.oss.accessKeyId");
            this.accessKeySecret = props.getProperty("aliyun.oss.accessKeySecret");
            this.bucketName = props.getProperty("aliyun.oss.bucketName");
            this.urlPrefix = props.getProperty("aliyun.oss.urlPrefix");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load oss.properties");
        }
    }

    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) return null;

        // 生成文件名: avatars/uuid.jpg
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = "avatars/" + UUID.randomUUID().toString().replace("-", "") + suffix;

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            InputStream inputStream = file.getInputStream();
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(inputStream.available());
            meta.setContentType(file.getContentType());
            meta.setCacheControl("no-cache");

            ossClient.putObject(bucketName, fileName, inputStream, meta);

            // 返回完整访问路径
            return urlPrefix + fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (ossClient != null) ossClient.shutdown();
        }
    }
}