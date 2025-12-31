package hdc.rjxy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import hdc.rjxy.pojo.AdminOpLog;
import hdc.rjxy.pojo.vo.AdminOpLogVO;

public interface AdminOpLogService {
    void saveLog(AdminOpLog log);
    Page<AdminOpLogVO> page(int page, int size, String opType, String keyword);
}