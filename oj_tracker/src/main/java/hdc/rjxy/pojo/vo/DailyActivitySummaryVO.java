package hdc.rjxy.pojo.vo;

import lombok.Data;

@Data
public class DailyActivitySummaryVO {
    private Integer submitTotal;
    private Integer acceptTotal;
    private Integer solvedTotal;
    private Integer activeDays;
}
