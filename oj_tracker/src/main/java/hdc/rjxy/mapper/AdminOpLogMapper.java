package hdc.rjxy.mapper;

import hdc.rjxy.pojo.vo.AdminOpLogVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminOpLogMapper {
    int insert(Long adminId, Long targetUserId, String opType, String ip, String remark);

    List<AdminOpLogVO> pageLogs(@Param("opType") String opType,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    Long countLogs(@Param("opType") String opType);
}
