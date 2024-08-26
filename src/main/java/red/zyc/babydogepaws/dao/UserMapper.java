package red.zyc.babydogepaws.dao;

import org.apache.ibatis.annotations.Insert;

/**
 * @author allurx
 */
public interface UserMapper {

    @Insert("""
            INSERT INTO user ( user_id, invite_link, x_api_key, friend_num )
            VALUES
            	(#{userId},#{inviteLink},#{xApiKey},#{friendNum})
            	ON DUPLICATE KEY UPDATE 
                        x_api_key = #{xApiKey},
                        friend_num=#{friendNum};
                        """)
    int saveOrUpdateUser(Integer userId, String inviteLink, String xApiKey, Integer friendNum);
}
