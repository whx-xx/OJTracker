package hdc.rjxy.mapper;

import hdc.rjxy.pojo.vo.MyPlatformAccountVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserPlatformAccountMapper {

    List<MyPlatformAccountVO> listMine(@Param("userId") Long userId);

    int upsert(@Param("userId") Long userId,
               @Param("platformId") Long platformId,
               @Param("identifierType") String identifierType,
               @Param("identifierValue") String identifierValue);

    int deleteOne(@Param("userId") Long userId,
                  @Param("platformId") Long platformId);

    String findIdentifierValue(@Param("userId") Long userId,
                               @Param("platformId") Long platformId);

}
