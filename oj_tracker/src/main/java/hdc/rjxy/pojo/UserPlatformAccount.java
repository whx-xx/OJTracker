package hdc.rjxy.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_platform_account")
public class UserPlatformAccount {
    // 这是一个中间表，通常使用联合主键 (user_id, platform_id)
    // MyBatis-Plus 默认假定有一个 'id' 主键，如果没有，我们在操作时需要使用 Wrapper

    private Long userId;
    private Long platformId;
    private String identifierType;
    private String identifierValue;
    private Integer verified;
    private LocalDateTime updatedAt;
}