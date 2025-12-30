package hdc.rjxy.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserSession implements Serializable {
    private Long id;
    private String username;
    private String studentNo;
    private String nickname;
    private String role;
    private Integer status;
    private Integer mustChangePassword;
    private String avatar;
}
