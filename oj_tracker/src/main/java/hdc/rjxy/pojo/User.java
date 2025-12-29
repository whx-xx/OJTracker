package hdc.rjxy.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user") // 1. 映射数据库表名
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    // 2. 指定主键策略 (AUTO = 数据库自增)
    @TableId(type = IdType.AUTO)
    private Long id;

    private String studentNo;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private Integer status;
    private String passwordHash;
    private Integer mustChangePassword;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}