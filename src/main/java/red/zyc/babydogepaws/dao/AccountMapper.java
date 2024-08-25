package red.zyc.babydogepaws.dao;

import org.apache.ibatis.annotations.Insert;
import red.zyc.babydogepaws.model.persistent.Account;

/**
 * @author allurx
 */
public interface AccountMapper {

    @Insert("""
            insert into account(user_id,invite_link) values(#{userId},#{inviteLink});
            """)
    int saveAccount(Account account);
}
