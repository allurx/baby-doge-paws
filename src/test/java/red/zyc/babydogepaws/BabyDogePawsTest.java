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
import red.zyc.babydogepaws.common.util.WebUtil;
import red.zyc.babydogepaws.dao.UserConfigMapper;
import red.zyc.babydogepaws.dao.UserMapper;
import red.zyc.babydogepaws.exception.BabyDogePawsApiException;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static red.zyc.kit.json.JsonOperator.JACKSON_OPERATOR;

/**
 * @author allurx
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BabyDogePawsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePawsTest.class);
    private static final HttpClient CLIENT = HttpClient.newBuilder().
            connectTimeout(Duration.ofSeconds(30L))
            .executor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("BabyDogePawsApiRequester-", 0).factory()))
            .build();

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private UserConfigMapper userConfigMapper;
    @Autowired
    private UserMapper userMapper;


    void userAgent() throws Exception{
        Path inputPath = Paths.get("C:\\Users\\zyc\\Downloads\\user-agents_safari_iphone_mobile-phone_ios_webkit.txt");
        Path outputPath = Paths.get("C:\\Users\\zyc\\Desktop\\user-agents.txt");
        Pattern pattern=Pattern.compile("^Mozilla/5\\.0 \\(iPhone; CPU iPhone OS (1[0-7]|10|11|12|13|14|15|16|17)(_\\d+)? like Mac OS X\\) AppleWebKit/(605\\.1\\.15|605\\.1\\.12|604\\.3\\.5|601\\.1\\.46) \\(KHTML, like Gecko\\) Version\\/(17\\.3|17\\.0|16\\.5|16\\.0|15\\.5|15\\.0|14\\.0|13\\.0|12\\.0|11\\.0|10\\.0) Mobile/\\w+ Safari/(605\\.1\\.15|605\\.1\\.12|604\\.1|601\\.1)$");
        var list = Files.lines(inputPath)
                .parallel()
                .filter(s -> pattern.matcher(s).find())
                .toList();
        Files.write(outputPath,list);
    }
    @Test
    void updateUserAgent() throws Exception {
        var userAgents = Files.lines(Paths.get("C:\\Users\\zyc\\Desktop\\user-agents.txt")).toList();
        userMapper.listBabyDogeUsers().parallelStream().forEach(user -> {
            if(user.id!=1 && user.id!=2 && user.id!=9){
                int index = Math.abs(user.id.hashCode()) % userAgents.size();
                userConfigMapper.updateUserAgent(user.id,userAgents.get(index)
                        .replaceAll(" Version/\\d+\\.\\d+", "")
                        .replaceAll(" Safari/\\d+\\.\\d+", "")
                        .trim()
                );
            }else {
                userConfigMapper.updateUserAgent(user.id,"Mozilla/5.0 (iPhone; CPU iPhone OS 17_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148");
            }
        });
    }



    @Test
    void getUser() {
        BabyDogePawsUser body = testRestTemplate.exchange(
                RequestEntity.get("/getUser?phoneNumber={?}", "19962006575").build(), new ParameterizedTypeReference<BabyDogePawsUser>() {
                }).getBody();
        assert body != null;
        LOGGER.info(JACKSON_OPERATOR.toJsonString(body));
    }

    @Test
    void listFriends() {
        Map<String, Object> body = testRestTemplate.exchange(
                RequestEntity.get("/listFriends?phoneNumber={?}", "19962006575").build(), new ParameterizedTypeReference<Map<String, Object>>() {
                }).getBody();
        assert body != null;
        LOGGER.info(JACKSON_OPERATOR.toJsonString(body));
    }

    @Test
    void listCardUpgradeInfo() {
        List<Map<String, String>> body = testRestTemplate.exchange(
                RequestEntity.get("/listCardUpgradeInfo?phoneNumber={?}", "19962006575").build(), new ParameterizedTypeReference<List<Map<String, String>>>() {
                }).getBody();
        assert body != null;
        LOGGER.info(JACKSON_OPERATOR.toJsonString(body));
    }

    @Test
    void test() {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://backend.babydogepawsbot.com/channels"))
                    .header("content-type", "application/json")
                    .header("x-api-key", "41123b6ff55a5922fe76f1a76ca2b3f27dffd63f9242066521c1696cd6a22d26")
                    .POST(HttpRequest.BodyPublishers.ofString(JACKSON_OPERATOR.toJsonString(Map.of("channel_id", 128129701))))
                    .build();
            IntStream.range(0, 100).parallel().forEach(value -> httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            LOGGER.warn("[采集任务失败]-{}", WebUtil.formatJsonResponse(response, true));
                        } else {
                            LOGGER.info("[采集任务成功]-{}", WebUtil.formatJsonResponse(response, true));
                        }
                    })
                    .join());
        }

    }




    void test1() throws IOException {
        Path root = Path.of("D:\\chrome-user-data-new");
        StringBuilder builder = new StringBuilder(
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


    static void pickDailyBonus(String xApiKey) {
        var data = CLIENT.sendAsync(HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/pickDailyBonus"))
                        .header("x-api-key", xApiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(""))
                        .build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> WebUtil.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("pickDailyBonus响应结果为空")))
                .join();

        LOGGER.info("pickDailyBonus: {}", JACKSON_OPERATOR.toJsonString(data));
    }

    static void mine(String xApiKey) {
        var data = CLIENT.sendAsync(HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/mine"))
                        .header("content-type", "application/json")
                        .header("x-api-key", xApiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(JACKSON_OPERATOR.toJsonString(Map.of("count", ThreadLocalRandom.current().nextInt(1, 11)))))
                        .build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> WebUtil.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("mine响应结果为空")))
                .join();

        LOGGER.info("mine: {}", JACKSON_OPERATOR.toJsonString(data));
    }

    static void upgradeCard(String xApiKey, int cardId) {
        var data = CLIENT.sendAsync(HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/cards"))
                        .header("content-type", "application/json")
                        .header("x-api-key", xApiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(JACKSON_OPERATOR.toJsonString(Map.of("id", cardId))))
                        .build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> WebUtil.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("upgradeCard响应结果为空")))
                .join();

        LOGGER.info("upgradeCard: {}", JACKSON_OPERATOR.toJsonString(data));
    }

    static void getMe(String xApiKey) {
        var data = CLIENT.sendAsync(HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/getMe"))
                        .header("x-api-key", xApiKey)
                        .GET()
                        .build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> WebUtil.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("getMe响应结果为空")))
                .join();

        LOGGER.info("getMe: {}", JACKSON_OPERATOR.toJsonString(data));
    }

    public static void main(String[] args) throws Exception {



    }


}
