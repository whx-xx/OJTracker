package hdc.rjxy.pojo.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateMyPlatformsReq {
    private List<Item> items;

    @Data
    public static class Item {
        private Long platformId;
        private String identifierType;
        private String identifierValue;
    }
}