package hdc.rjxy.controller;

import hdc.rjxy.common.PageResult;
import hdc.rjxy.common.R;
import hdc.rjxy.pojo.vo.AdminOpLogVO;
import hdc.rjxy.service.AdminOpLogService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/op-logs")
public class AdminOpLogController {

    private final AdminOpLogService adminOpLogService;

    public AdminOpLogController(AdminOpLogService adminOpLogService) {
        this.adminOpLogService = adminOpLogService;
    }

    @GetMapping
    public R<PageResult<AdminOpLogVO>> page(@RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                           @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize,
                                           @RequestParam(value = "opType", required = false) String opType) {
        return R.ok(adminOpLogService.pageLogs(page, pageSize, opType));
    }
}

