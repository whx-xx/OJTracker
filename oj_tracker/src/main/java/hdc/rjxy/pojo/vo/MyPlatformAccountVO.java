package hdc.rjxy.pojo.vo;

import lombok.Data;

@Data
public class MyPlatformAccountVO {
    private Long platformId;
    private String platformCode;
    private String platformName;
    private String identifierType;
    private String identifierValue;
    private Integer verified;
}