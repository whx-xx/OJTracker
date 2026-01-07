package hdc.rjxy.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新题目笔记请求")
public class UpdateProblemNotesReq {

    @Schema(description = "solved_problem 表的主键 ID")
    private Long id;

    @Schema(description = "笔记内容，支持 Markdown 和 LaTeX 语法")
    private String notes;
}