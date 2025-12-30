package hdc.rjxy.cf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
public class CfUserInfoResponse {
    private String status;
    private List<User> result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    // 忽略未知字段
    public static class User {
        private String handle;
        private Integer rating;     // 可能为空（未评级）
        private Integer maxRating;  // 可能为空
        private String rank;        // 可选
        private String maxRank;     // 可选
    }
}
