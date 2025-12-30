package hdc.rjxy.pojo.vo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class HeatmapDayVO {
    private LocalDate day;
    private int submitCnt;
    private int acceptCnt;
    private int solvedCnt;
}