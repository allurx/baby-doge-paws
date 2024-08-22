package red.zyc.babydogepaws.dao;

import org.apache.ibatis.annotations.Select;
import red.zyc.babydogepaws.model.persistent.TelegramUser;

import java.util.List;

/**
 * @author allurx
 */
public interface UserMapper {

    @Select("select * from telegram_user")
    List<TelegramUser> listTelegramUsers();
}
