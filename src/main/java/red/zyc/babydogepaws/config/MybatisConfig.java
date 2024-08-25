package red.zyc.babydogepaws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import red.zyc.babydogepaws.dao.TelegramUserMapper;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;

import java.util.List;

/**
 * @author allurx
 */
@Configuration
public class MybatisConfig {

    private final TelegramUserMapper telegramUserMapper;

    public MybatisConfig(TelegramUserMapper telegramUserMapper) {
        this.telegramUserMapper = telegramUserMapper;
    }

    @Bean("users")
    public List<BabyDogePawsUser> listBabyDogeUsers() {
        return telegramUserMapper.listBabyDogeUsers();
    }

}
