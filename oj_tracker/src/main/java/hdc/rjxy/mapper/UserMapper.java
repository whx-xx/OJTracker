package hdc.rjxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hdc.rjxy.pojo.User;
import org.apache.ibatis.annotations.Mapper;

// 继承 BaseMapper，泛型指定为实体类 User
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // 你仍然可以在这里定义自定义查询，并在 XML 中实现
    // 例如原来的复杂统计查询：
    // AdminUserStatsVO getAdminUserStats();
}