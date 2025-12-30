package hdc.rjxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hdc.rjxy.pojo.RatingSnapshot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RatingSnapshotMapper extends BaseMapper<RatingSnapshot> {
    // 使用 MyBatis-Plus Wrapper 代替原本的 findLast，所以这里可以是空的
}