package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserAdminVO {
    private Long id;
    private String studentNo;
    private String username;
    private String nickname;
    private String role;
    private Integer status; // 1=正常, 0=禁用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}