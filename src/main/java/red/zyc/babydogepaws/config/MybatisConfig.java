package red.zyc.babydogepaws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import red.zyc.babydogepaws.dao.UserMapper;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;

import java.util.List;

/**
 * @author allurx
 */
@Configuration
public class MybatisConfig {

    private final UserMapper userMapper;

    public MybatisConfig(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Bean("users")
    public List<BabyDogePawsUser> listBabyDogeUsers() {
        return userMapper.listBabyDogeUsers();
    }

}
