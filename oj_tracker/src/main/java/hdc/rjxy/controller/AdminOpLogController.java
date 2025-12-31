package hdc.rjxy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.common.R;
import hdc.rjxy.pojo.vo.AdminOpLogVO;
import hdc.rjxy.service.AdminOpLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-操作日志", description = "查看管理员操作记录")
@RestController
@RequestMapping("/api/admin/logs")
public class AdminOpLogController {

    private final AdminOpLogService adminOpLogService;

    public AdminOpLogController(AdminOpLogService adminOpLogService) {
        this.adminOpLogService = adminOpLogService;
    }

    @Operation(summary = "获取日志列表")
    @GetMapping
    public R<Page<AdminOpLogVO>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return R.ok(adminOpLogService.page(page, size));
    }
}