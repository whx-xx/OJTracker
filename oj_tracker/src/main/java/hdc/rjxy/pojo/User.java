package hdc.rjxy.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;

    /** 学号（普通用户唯一，admin 可为 null） */
    private String studentNo;

    /** 用户名（登录名/展示名） */
    private String username;

    /** 昵称（管理员可改为班级姓名） */
    private String nickname;

    /** 角色：ADMIN / USER */
    private String role;

    /** 状态：1=启用，0=禁用 */
    private Integer status;

    /** BCrypt 密码 hash */
    private String passwordHash;

    /** 是否必须修改密码：1=是，0=否 */
    private Integer mustChangePassword;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
