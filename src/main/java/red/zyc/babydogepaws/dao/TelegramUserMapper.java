package red.zyc.babydogepaws.dao;

import org.apache.ibatis.annotations.Select;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;

import java.util.List;

/**
 * @author allurx
 */
public interface TelegramUserMapper {

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

}
