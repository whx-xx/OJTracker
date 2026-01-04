package hdc.rjxy.pojo.vo;

import lombok.Data;

/**
 * 同步用户变更详情 VO
 * 用于展示成功同步且有数据变化的用户记录
 */
@Data
public class SyncUserChangeVO {
    private Long userId;
    private String handle;      // 用户名/Handle
    private Integer newSub;     // 新增提交数
    private Integer newSolved;  // 新增AC数
    private String rawDetails;  // 原始详情字符串
}