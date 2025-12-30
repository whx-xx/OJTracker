package hdc.rjxy.pojo.vo;

import lombok.Data;

@Data
public class DailyActivitySummaryVO {
    private int submitTotal;
    private int acceptTotal;
    private int solvedTotal;
    private int activeDays;
}