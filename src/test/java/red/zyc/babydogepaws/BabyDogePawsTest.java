package red.zyc.babydogepaws;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import red.zyc.babydogepaws.common.util.ApplicationContextHolder;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.toolkit.json.Json;

import java.util.List;
import java.util.Map;

/**
 * @author allurx
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BabyDogePawsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePawsTest.class);

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void getUser() {
        BabyDogePawsUser body = testRestTemplate.exchange(
                RequestEntity.get("/getUser?phoneNumber={?}", "19962006575").build(), new ParameterizedTypeReference<BabyDogePawsUser>() {
                }).getBody();
        assert body != null;
        LOGGER.info(Json.JACKSON_OPERATOR.toJsonString(body));
    }

    @Test
    void listFriends() {
        Map<String, Object> body = testRestTemplate.exchange(
                RequestEntity.get("/listFriends?phoneNumber={?}", "19962006575").build(), new ParameterizedTypeReference<Map<String, Object>>() {
                }).getBody();
        assert body != null;
        LOGGER.info(Json.JACKSON_OPERATOR.toJsonString(body));
    }

    @Test
    void listCardUpgradeInfo() {
        List<Map<String, String>> body = testRestTemplate.exchange(
                RequestEntity.get("/listCardUpgradeInfo?phoneNumber={?}", "19962006575").build(), new ParameterizedTypeReference<List<Map<String, String>>>() {
                }).getBody();
        assert body != null;
        LOGGER.info(Json.JACKSON_OPERATOR.toJsonString(body));
    }

    @Test
    void test(){
        String property = ApplicationContextHolder.getProperty("chrome.root-data-dir", String.class);
        System.out.println(property);
    }

}
