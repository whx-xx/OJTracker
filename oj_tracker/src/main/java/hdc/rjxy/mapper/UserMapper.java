package hdc.rjxy.mapper;

import hdc.rjxy.pojo.User;
import hdc.rjxy.pojo.vo.AdminUserStatsVO;
import hdc.rjxy.pojo.vo.UserAdminVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {

    User findByUsername(@Param("username") String username);

    User findByStudentNo(@Param("studentNo") String studentNo);

    User findById(@Param("id") Long id);

    int insertAdmin(User user);


    int insertUser(User user);

    int updatePasswordAndMustChange(@Param("id") Long id,
                                    @Param("passwordHash") String passwordHash,
                                    @Param("mustChangePassword") Integer mustChangePassword);

    int updateMustChangePassword(@Param("id") Long id, @Param("mustChangePassword") Integer mustChangePassword);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    List<User> listAll();

    List<UserAdminVO> pageAdminList(@Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    Long countAdminList(@Param("keyword") String keyword);

    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);
    int updateUsername(@Param("id") Long id, @Param("username") String username);

    AdminUserStatsVO getAdminUserStats();
}
