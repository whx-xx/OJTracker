    package hdc.rjxy.mapper;

    import hdc.rjxy.pojo.vo.TeamMemberSimpleVO;
    import org.apache.ibatis.annotations.MapKey;
    import org.apache.ibatis.annotations.Param;
    import java.util.List;
    import java.util.Map;

    public interface TeamMemberMapper {
        List<TeamMemberSimpleVO> listEnabledMembers(
                @Param("teamCode") String teamCode
        );

        int addMember(@Param("teamId") Long teamId, @Param("userId") Long userId);


    }
