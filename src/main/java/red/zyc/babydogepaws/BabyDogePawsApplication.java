package red.zyc.babydogepaws;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import red.zyc.babydogepaws.game.BabyDogePaws;

/**
 * @author allurx
 */
@MapperScan
@ConfigurationPropertiesScan
@SpringBootApplication
public class BabyDogePawsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BabyDogePawsApplication.class, args)
                .getBean(BabyDogePaws.class)
                .bootstrap();
    }

}
