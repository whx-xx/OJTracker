package hdc.rjxy.pojo.dto;

import lombok.Data;

@Data
public class LoginReq {
    private String username; // admin or 学号（你决定用哪个字段登录）
    private String password;
}
