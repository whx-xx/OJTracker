package hdc.rjxy.pojo.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateProblemTagsReq {
    // solved_problem 表的主键 ID
    private Long id;

    // 前端传来的标签列表，如 ["DP", "Math"]
    private List<String> tags;
}