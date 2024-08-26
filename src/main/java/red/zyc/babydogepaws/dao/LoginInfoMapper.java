package red.zyc.babydogepaws.dao;

import org.apache.ibatis.annotations.Insert;

import java.time.LocalDateTime;

/**
 * @author allurx
 */
public interface LoginInfoMapper {

    @Insert("""
            INSERT INTO login_info ( user_id, time, auth_param )
            VALUES
            	(#{userId},#{time},#{authParam})
            	ON DUPLICATE KEY UPDATE 
                        time = #{time},
                        auth_param=#{authParam};
                        """)
    int saveOrUpdateLoginInfo(Integer userId, LocalDateTime time, String authParam);
}
