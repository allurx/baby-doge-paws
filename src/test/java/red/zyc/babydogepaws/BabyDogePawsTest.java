package red.zyc.babydogepaws;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import red.zyc.babydogepaws.common.constant.Constants;
import red.zyc.babydogepaws.common.util.Https;
import red.zyc.babydogepaws.exception.BabyDogePawsApiException;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.toolkit.json.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static red.zyc.toolkit.json.Json.JACKSON_OPERATOR;

/**
 * @author allurx
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BabyDogePawsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePawsTest.class);
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30L)).build();

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
    void test() {
        var data = CLIENT.sendAsync(HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/channels"))
                        .header("content-type", "application/json")
                        .header("x-api-key", "fb2824f774c9ce26cc9eb961a043a3c55bd9be643d6c3281d43147e028e9a250")
                        .POST(HttpRequest.BodyPublishers.ofString(JACKSON_OPERATOR.toJsonString(Map.of("channelId", 158095140))))
                        .build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("pickChannel响应结果为空")))
                .join();

        System.out.println(JACKSON_OPERATOR.toJsonString(data));

    }

    public static void main(String[] args)throws Exception {
        Path root = Path.of("D:\\chrome-user-data-new");
        StringBuilder builder=new StringBuilder(
                "insert into telegram_user(country,area_code,phone_number,source,banned,password_reset,email_reset) values ");
        Files.walk(root, 1).filter(path -> !path.equals(root) && path.toFile().isDirectory())
                .forEach(path -> {
                    builder.append("(")
                            .append("'美国'")
                            .append(",")
                            .append("1")
                            .append(",")
                            .append(path.getFileName().toString().substring(2))
                            .append(",")
                            .append(3)
                            .append(",")
                            .append(0)
                            .append(",")
                            .append(1)
                            .append(",")
                            .append(0)
                            .append(")")
                            .append(",");
        });
        System.out.println(builder);


    }


}
