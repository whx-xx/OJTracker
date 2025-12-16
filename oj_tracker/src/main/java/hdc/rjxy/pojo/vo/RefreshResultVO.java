package hdc.rjxy.pojo.vo;

import lombok.Data;

@Data
public class RefreshResultVO {
    private int fetched;    // 本次从 CF 拉到多少条（count 或更少）
    private int inserted;   // 实际新增写入多少条（增量）
}
