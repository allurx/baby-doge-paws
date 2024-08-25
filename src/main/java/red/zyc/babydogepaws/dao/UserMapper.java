package red.zyc.babydogepaws.dao;

import org.apache.ibatis.annotations.Select;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.babydogepaws.model.persistent.TelegramUser;

import java.util.List;

/**
 * @author allurx
 */
public interface UserMapper {

    @Select("select * from telegram_user")
    List<TelegramUser> listTelegramUsers();

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
             	a.created_time,
             	a.modified_time,
             	b.auth_param
             FROM
             	telegram_user a
             	LEFT JOIN game_login_info b ON a.id = b.user_id
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
             	a.created_time,
             	a.modified_time,
             	b.auth_param
             FROM
             	telegram_user a
             	LEFT JOIN game_login_info b ON a.id = b.user_id
             WHERE
             	a.banned = 0 and a.id=#{userId}
             ORDER BY
             	a.id ASC
            """)
    BabyDogePawsUser getBabyDogeUser(Integer userId);
}
