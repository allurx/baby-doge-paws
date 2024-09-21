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
            	c.auth_param,
	            d.maximum_card_upgrade_price,
                d.user_agent
            FROM
            	telegram_user a
            	LEFT JOIN user b ON a.id = b.user_id
            	LEFT JOIN login_info c ON a.id = c.user_id
                left join user_config d on a.id=d.user_id
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
            	c.auth_param,
		        d.maximum_card_upgrade_price,
                d.user_agent
            FROM
            	telegram_user a
            	LEFT JOIN user b ON a.id = b.user_id
            	LEFT JOIN login_info c ON a.id = c.user_id
                left join user_config d on a.id=d.user_id
            WHERE
            	a.banned = 0
                and a.phone_number = #{phoneNumber}
            """)
    BabyDogePawsUser getBabyDogeUser(String phoneNumber);

    @Insert("""
            INSERT INTO user ( user_id, balance,profit_per_hour,level,invite_link, x_api_key, friend_num )
            VALUES
            	(#{userId},#{balance},#{profitPerHour},#{level},#{inviteLink},#{xApiKey},#{friendNum})
            	ON DUPLICATE KEY UPDATE 
                     balance=#{balance},
                     profit_per_hour=#{profitPerHour},
                     `level`=#{level},
                        x_api_key = #{xApiKey},
                        friend_num=#{friendNum};
                        """)
    int saveOrUpdateUser(Integer userId,
                         long balance,
                         int profitPerHour,
                         int level,
                         String inviteLink,
                         String xApiKey,
                         Integer friendNum);
}
