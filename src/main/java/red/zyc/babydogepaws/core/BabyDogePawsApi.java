package red.zyc.babydogepaws.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import red.zyc.babydogepaws.common.constant.Constants;
import red.zyc.babydogepaws.common.util.ApplicationContextHolder;
import red.zyc.babydogepaws.common.util.Https;
import red.zyc.babydogepaws.common.util.Mails;
import red.zyc.babydogepaws.exception.BabyDogePawsApiException;
import red.zyc.babydogepaws.exception.BabyDogePawsException;
import red.zyc.babydogepaws.model.PickChannel;
import red.zyc.babydogepaws.model.UpgradeCard;
import red.zyc.babydogepaws.model.User;
import red.zyc.babydogepaws.model.request.MineRequestParam;
import red.zyc.babydogepaws.model.request.PickChannelRequestParam;
import red.zyc.babydogepaws.model.request.UpgradeCardRequestParam;
import red.zyc.toolkit.json.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author allurx
 */
@Service
public class BabyDogePawsApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePawsApi.class);
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30L)).build();

    /**
     * 如果x-api-key过期的话则需要重新授权一下
     *
     * @param user {@link User}
     * @param code http响应码
     */
    private void reAuthorizeIfNecessary(User user, int code) {
        if (code == 401) {
            LOGGER.error("无效的x-api-key: {}", user.xApiKey());
            authorize(user);
        }
    }

    /**
     * 游戏授权
     *
     * @param user {@link User}
     * @return 用户信息
     */
    public Map<String, Object> authorize(User user) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://backend.babydogepawsbot.com/authorize"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(user.authParam))
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply((response) -> {
                    if (response.statusCode() != 200) {

                        LOGGER.error("[授权失败]-{}:{}:{}", user.name, user.authParam, Https.formatJsonResponse(response, true));

                        // authParam过期了，需要重新登录一下
                        if (response.statusCode() == 400) {

                            ApplicationContextHolder.getBean(BabyDogePaws.class).playBabyDogePaws(user, 0);
                            return new HashMap<>();

                            // 服务器维护了，直接返回，等待下一个定时任务执行直到服务器恢复
                        } else if (response.statusCode() == 503) {
                            return new HashMap<>();
                        } else {
                            throw new BabyDogePawsException("授权失败，未知的http响应码: " + Https.formatJsonResponse(response, true));
                        }
                    } else {
                        LOGGER.info("[授权成功]-{}:{}:{}", user.name, user.authParam, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE).map((authData) -> {
                            user.data = authData;
                            return authData;
                        }).orElseThrow(() -> new BabyDogePawsApiException("authorize响应结果为空"));
                    }
                }).join();
    }

    /**
     * 获取用户信息
     *
     * @param user {@link User}
     * @return 用户信息
     */
    public Map<String, Object> getMe(User user) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://backend.babydogepawsbot.com/getMe"))
                .header("x-api-key", user.xApiKey())
                .GET()
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply((response) -> {
                    if (response.statusCode() != 200) {
                        LOGGER.error("[获取用户信息失败]-{}:{}", user.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(user, response.statusCode());
                        return new HashMap<>();
                    } else {
                        LOGGER.info("[获取用户信息成功]-{}:{}", user.name, Https.formatJsonResponse(response, false));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE).
                                orElseThrow(() -> new BabyDogePawsApiException("getMe响应结果为空"));
                    }
                }).join();
    }

    /**
     * 采集每日奖励
     *
     * @param user {@link User}
     * @return 每日奖励信息
     */
    public Map<String, Object> pickDailyBonus(User user) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://backend.babydogepawsbot.com/pickDailyBonus"))
                .header("x-api-key", user.xApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply((response) -> {
                    if (response.statusCode() != 200) {
                        LOGGER.error("[采集每日奖励失败]-{}:{}", user.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(user, response.statusCode());
                        Mails.sendTextMail(user.name + "采集每日奖励失败", Https.formatJsonResponse(response, true));
                        return new HashMap<>();
                    } else {
                        LOGGER.info("[采集每日奖励成功]-{}:{}", user.name, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("pickDailyBonus响应结果为空"));
                    }
                }).join();
    }

    /**
     * 获取每日奖励信息
     *
     * @param user {@link User}
     * @return 每日奖励信息
     */
    public Map<String, Object> getDailyBonuses(User user) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://backend.babydogepawsbot.com/getDailyBonuses"))
                .header("x-api-key", user.xApiKey())
                .GET()
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply((response) -> {
                    if (response.statusCode() != 200) {
                        LOGGER.error("[获取每日奖励内容失败]-{}:{}", user.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(user, response.statusCode());
                        return new HashMap<>();
                    } else {
                        LOGGER.info("[获取每日奖励内容成功]-{}:{}", user.name, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("getDailyBonuses响应结果为空"));
                    }
                }).join();
    }

    /**
     * 获取所有卡片
     *
     * @param user {@link User}
     * @return 所有卡片
     */
    public List<Map<String, Object>> listCards(User user) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://backend.babydogepawsbot.com/cards"))
                .header("x-api-key", user.xApiKey())
                .GET()
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<List<Map<String, Object>>>thenApply((response) -> {
                    if (response.statusCode() != 200) {
                        LOGGER.error("[获取卡片列表失败]-{}:{}", user.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(user, response.statusCode());
                        return new ArrayList<>();
                    } else {
                        LOGGER.info("[获取卡片列表成功]-{}:{}", user.name, Https.formatJsonResponse(response, false));
                        return Https.parseJsonResponse(response, Constants.LIST_OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("listCards响应结果为空"));
                    }
                }).join();
    }

    /**
     * 升级卡片
     *
     * @param user        {@link User}
     * @param upgradeCard {@link UpgradeCard}
     * @return 卡片升级后的信息，其中包括用户信息和升级后的所有卡片信息
     */
    public Map<String, Object> upgradeCard(User user, UpgradeCard upgradeCard) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://backend.babydogepawsbot.com/cards"))
                .header("content-type", "application/json")
                .header("x-api-key", user.xApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(Json.JACKSON_OPERATOR.toJsonString(new UpgradeCardRequestParam(upgradeCard.card().id()))))
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply((response) -> {
                    if (response.statusCode() != 200) {
                        LOGGER.error("[卡片升级失败]-{}:{}:{}:{}:{}", user.name, upgradeCard.balance(), upgradeCard.card().id(), upgradeCard.card().upgradeCost(), Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(user, response.statusCode());
                        return new HashMap<>();
                    } else {
                        LOGGER.info("[卡片升级成功]-{}:{}:{}:{}:{}", user.name, upgradeCard.balance(), upgradeCard.card().id(), upgradeCard.card().upgradeCost(), Https.formatJsonResponse(response, false));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("cards响应结果为空"));
                    }
                }).join();
    }

    /**
     * 挖矿
     *
     * @param user  {@link User}
     * @param param {@link MineRequestParam}
     * @return 挖矿后的信息
     */
    public Map<String, Object> mine(User user, MineRequestParam param) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://backend.babydogepawsbot.com/mine"))
                .header("content-type", "application/json")
                .header("x-api-key", user.xApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(Json.JACKSON_OPERATOR.toJsonString(param)))
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply((response) -> {
                    if (response.statusCode() != 200) {
                        LOGGER.error("[挖矿失败]-{}:{}:{}", user.name, Json.JACKSON_OPERATOR.toJsonString(param), Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(user, response.statusCode());
                        return new HashMap<>();
                    } else {
                        LOGGER.info("[挖矿成功]-{}:{}:{}", user.name, Json.JACKSON_OPERATOR.toJsonString(param), Https.formatJsonResponse(response, false));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("mine响应结果为空"));
                    }
                }).join();
    }

    /**
     * 获取所有任务
     *
     * @param user {@link User}
     * @return 所有任务
     */
    public Map<String, Object> listChannels(User user) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://backend.babydogepawsbot.com/channels"))
                .header("x-api-key", user.xApiKey())
                .GET()
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply((response) -> {
                    if (response.statusCode() != 200) {
                        LOGGER.error("[获取任务列表失败]-{}:{}", user.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(user, response.statusCode());
                        return new HashMap<>();
                    } else {
                        LOGGER.info("[获取任务列表成功]-{}:{}", user.name, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("listChannels响应结果为空"));
                    }
                }).join();
    }

    /**
     * 采集任务奖励
     *
     * @param user        {@link User}
     * @param pickChannel {@link PickChannel}
     * @return 采集任务奖励响应
     */
    public Map<String, Object> pickChannel(User user, PickChannel pickChannel) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://backend.babydogepawsbot.com/channels"))
                .header("content-type", "application/json")
                .header("x-api-key", user.xApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(Json.JACKSON_OPERATOR.toJsonString(new PickChannelRequestParam(pickChannel.channel().id()))))
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply((response) -> {
                    if (response.statusCode() != 200) {
                        LOGGER.error("[采集任务奖励失败]-{}:{}:{}:{}", user.name, pickChannel.channel().id(), pickChannel.channel().inviteLink(), Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(user, response.statusCode());
                        return new HashMap<>();
                    } else {
                        LOGGER.info("[采集任务奖励成功]-{}:{}:{}:{}", user.name, pickChannel.channel().id(), pickChannel.channel().inviteLink(), Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("pickChannel响应结果为空"));
                    }
                }).join();
    }

    /**
     * 采集促销奖励
     *
     * @param user {@link User}
     * @return 采集促销奖励响应
     */
    public Map<String, Object> pickPromo(User user) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://backend.babydogepawsbot.com/promo"))
                .header("x-api-key", user.xApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply((response) -> {
                    if (response.statusCode() != 200) {
                        LOGGER.error("[参与促销失败]-{}:{}", user.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(user, response.statusCode());
                        Mails.sendTextMail(user.name + "参与促销失败", Https.formatJsonResponse(response, true));
                        return new HashMap<>();
                    } else {
                        LOGGER.info("[参与促销成功]-{}:{}", user.name, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("pickPromo响应结果为空"));
                    }
                }).join();
    }

    /**
     * 获取促销信息
     *
     * @param user {@link User}
     * @return 响应
     */
    public Map<String, Object> getPromo(User user) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://backend.babydogepawsbot.com/promo"))
                .header("x-api-key", user.xApiKey()).GET()
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply((response) -> {
                    if (response.statusCode() != 200) {
                        LOGGER.error("[获取促销信息失败]-{}:{}", user.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(user, response.statusCode());
                        return new HashMap<>();
                    } else {
                        LOGGER.info("[获取促销信息成功]-{}:{}", user.name, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("getPromo响应结果为空"));
                    }
                }).join();
    }

}
