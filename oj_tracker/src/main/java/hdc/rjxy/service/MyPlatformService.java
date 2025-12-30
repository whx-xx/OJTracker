package hdc.rjxy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import hdc.rjxy.pojo.UserPlatformAccount;
import hdc.rjxy.pojo.dto.UpdateMyPlatformsReq;
import hdc.rjxy.pojo.vo.MyPlatformAccountVO;

import java.util.List;

public interface MyPlatformService extends IService<UserPlatformAccount> {
    List<MyPlatformAccountVO> listMine(Long userId);
    void updateMine(Long userId, UpdateMyPlatformsReq req);
}