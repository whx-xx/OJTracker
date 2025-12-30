package hdc.rjxy.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("platform")
public class Platform {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private Integer enabled;
}