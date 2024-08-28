package red.zyc.babydogepaws.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;

import java.util.List;

/**
 * @author allurx
 */
public interface UserMapper {

    @Select("""
            SELECT
            	a.id,
            	a.country,
            	a.area_code,
            	a.phone_number,
            	a.source,
            	a.banned,
            	a.password_reset,
            	a.email_reset,
            	b.invite_link,
            	b.x_api_key,
            	b.friend_num,
            	c.auth_param
            FROM
            	telegram_user a
            	LEFT JOIN user b ON a.id = b.user_id
            	LEFT JOIN login_info c ON a.id = c.user_id
            WHERE
            	a.banned = 0
            ORDER BY
            	a.id ASC
            """)
    List<BabyDogePawsUser> listBabyDogeUsers();

    @Select("""
            SELECT
            	a.id,
            	a.country,
            	a.area_code,
            	a.phone_number,
            	a.source,
            	a.banned,
            	a.password_reset,
            	a.email_reset,
            	b.invite_link,
            	b.x_api_key,
            	b.friend_num,
            	c.auth_param
            FROM
            	telegram_user a
            	LEFT JOIN user b ON a.id = b.user_id
            	LEFT JOIN login_info c ON a.id = c.user_id
            WHERE
            	a.banned = 0
                and a.phone_number = #{phoneNumber}
            """)
    BabyDogePawsUser getBabyDogeUser(String phoneNumber);

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
