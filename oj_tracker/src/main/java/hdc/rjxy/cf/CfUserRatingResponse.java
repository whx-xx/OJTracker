package hdc.rjxy.cf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfUserRatingResponse {

    private String status;
    private List<Item> result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private Long contestId;
        private String contestName;
        private Integer oldRating;
        private Integer newRating;
        private Long ratingUpdateTimeSeconds;
    }
}
