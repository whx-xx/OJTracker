package hdc.rjxy.cf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfUserStatusResponse {
    private String status;
    private List<Submission> result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Submission {
        private Long id; // CF submission id
        private Long creationTimeSeconds;
        private String verdict;
        private Problem problem;
    }


    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Problem {
        private Integer contestId;
        private String index;
        private String name;
    }
}
