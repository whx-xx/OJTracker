package hdc.rjxy.mapper;

import org.apache.ibatis.annotations.Param;

public interface TeamMapper {
    Long findIdByCode(@Param("code") String code);
}
