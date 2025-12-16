package hdc.rjxy.service;

import hdc.rjxy.common.PageResult;
import hdc.rjxy.mapper.AdminOpLogMapper;
import hdc.rjxy.pojo.vo.AdminOpLogVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminOpLogService {

    private final AdminOpLogMapper adminOpLogMapper;

    public AdminOpLogService(AdminOpLogMapper adminOpLogMapper) {
        this.adminOpLogMapper = adminOpLogMapper;
    }

    public PageResult<AdminOpLogVO> pageLogs(int page, int pageSize, String opType) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;

        int offset = (page - 1) * pageSize;

        Long total = adminOpLogMapper.countLogs(opType);
        List<AdminOpLogVO> list = adminOpLogMapper.pageLogs(opType, offset, pageSize);
        return PageResult.of(total == null ? 0 : total, list);
    }
}

