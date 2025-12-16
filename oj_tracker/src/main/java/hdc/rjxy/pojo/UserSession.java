package hdc.rjxy.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserSession implements Serializable {
    private Long id;
    private String username;
    private String studentNo;
    private String nickname;
    private String role;              // ADMIN / USER
    private Integer status;           // 1/0
    private Integer mustChangePassword; // 1/0
}
