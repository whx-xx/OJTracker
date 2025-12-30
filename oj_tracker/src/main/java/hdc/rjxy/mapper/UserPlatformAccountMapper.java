package hdc.rjxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hdc.rjxy.pojo.UserPlatformAccount;
import hdc.rjxy.pojo.vo.MyPlatformAccountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserPlatformAccountMapper extends BaseMapper<UserPlatformAccount> {

    /**
     * 查询用户绑定的平台列表（包含平台信息）
     * 这是一个多表关联查询，使用 @Select 注解直接实现，替代 XML
     */
    @Select("SELECT " +
            "p.id AS platformId, " +
            "p.code AS platformCode, " +
            "p.name AS platformName, " +
            "a.identifier_type AS identifierType, " +
            "a.identifier_value AS identifierValue, " +
            "a.verified AS verified " +
            "FROM platform p " +
            "LEFT JOIN user_platform_account a ON a.platform_id = p.id AND a.user_id = #{userId} " +
            "WHERE p.enabled = 1 " +
            "ORDER BY p.id ASC")
    List<MyPlatformAccountVO> listMine(Long userId);
}