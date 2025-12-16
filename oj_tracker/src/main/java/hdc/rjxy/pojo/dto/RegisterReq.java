package hdc.rjxy.pojo.dto;

import lombok.Data;

@Data
public class RegisterReq {
    private String studentNo;
    private String username;  // 用户名（展示用）
    private String password;
}
