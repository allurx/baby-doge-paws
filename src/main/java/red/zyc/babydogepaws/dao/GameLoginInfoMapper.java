package red.zyc.babydogepaws.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import red.zyc.babydogepaws.model.persistent.GameLoginInfo;

import java.util.List;

/**
 * @author allurx
 */
public interface GameLoginInfoMapper {

    @Select("select * from game_login_info")
    List<GameLoginInfo> listGameLoginInfos();

    @Insert("""
                    insert into game_login_info(user_id,auth_param,duration) values(#{userId},#{authParam},#{duration})
                        ON DUPLICATE KEY UPDATE auth_param = #{authParam},duration=#{duration};
            """)
    int saveOrUpdateGameLoginInfo(GameLoginInfo gameLoginInfo);
}
