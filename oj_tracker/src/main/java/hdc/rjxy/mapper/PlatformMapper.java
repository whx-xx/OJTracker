package hdc.rjxy.mapper;

import hdc.rjxy.pojo.Platform;
import org.apache.ibatis.annotations.Param;
import java.util.Map;

public interface PlatformMapper {

    Platform findEnabledById(@Param("id") Long id);

    Long findIdByCode(String code);
}
