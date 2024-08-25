package red.zyc.babydogepaws.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import red.zyc.babydogepaws.common.constant.Constants;
import red.zyc.babydogepaws.common.util.ApplicationContextHolder;
import red.zyc.babydogepaws.common.util.Https;
import red.zyc.babydogepaws.exception.BabyDogePawsApiException;
import red.zyc.babydogepaws.model.BabyDogePawsAccount;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
import red.zyc.babydogepaws.model.request.PickChannel;
import red.zyc.babydogepaws.model.request.UpgradeCard;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static red.zyc.babydogepaws.common.constant.BabyDogePawsGame.Request.*;

/**
 * @author allurx
 */
@Service
public class BabyDogePawsApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePawsApi.class);
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30L)).build();
    private static final ConcurrentHashMap<Integer, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    /**
     * 获取每个{@link BabyDogePawsUser}的锁，如果不存在则创建新锁
     *
     * @param userId {@link BabyDogePawsUser#id}
     * @return {@link BabyDogePawsUser}的锁
     */
    private static ReentrantLock getLock(Integer userId) {
        return LOCKS.computeIfAbsent(userId, k -> new ReentrantLock());
    }

    /**
     * 如果x-api-key过期的话则需要重新授权一下
     *
     * @param account {@link BabyDogePawsAccount}
     * @param code    http响应码
     */
    private void reAuthorizeIfNecessary(BabyDogePawsAccount account, int code) {
        if (code == 401) {
            authorize(new BabyDogePawsGameRequestParam(account));
        }
    }

    /**
     * 游戏授权
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 用户信息
     */
    public Map<String, Object> authorize(BabyDogePawsGameRequestParam param) {
        param.account.dataValid = false;
        var lock = getLock(param.account.user.id);
        lock.lock();
        try {
            // 其它线程可能已经将dataValid设置为true了
            return param.account.dataValid ? param.account.data :
                    CLIENT.sendAsync(AUTHORIZE.build(param), HttpResponse.BodyHandlers.ofString())
                            .<Map<String, Object>>thenApply(response -> {
                                if (response.statusCode() != 200) {

                                    LOGGER.warn("[授权失败]-{}:{}:{}", param.account.name, param.account.user.authParam, Https.formatJsonResponse(response, true));

                                    // authParam过期了或者不正确，需要重新登录一下
                                    if (response.statusCode() == 400) {
                                        ApplicationContextHolder.getBean(BabyDogePaws.class).playBabyDogePaws(param.account, 0);
                                        return new HashMap<>();
                                    }

                                    // 其它错误码直接返回，等待下一个定时任务执行直到服务器恢复
                                    return new HashMap<>();

                                } else {
                                    LOGGER.info("[授权成功]-{}:{}:{}", param.account.name, param.account.user.authParam, Https.formatJsonResponse(response, true));
                                    return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE).map((authData) -> {
                                        param.account.data = authData;
                                        param.account.dataValid = true;
                                        return authData;
                                    }).orElseThrow(() -> new BabyDogePawsApiException("authorize响应结果为空"));
                                }
                            })
                            .join();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取用户信息
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 用户信息
     */
    public Map<String, Object> getMe(BabyDogePawsGameRequestParam param) {
        return CLIENT.sendAsync(GET_ME.build(param), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[获取用户信息失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(param.account, response.statusCode());
                        return response.statusCode() == 401 ? getMe(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[获取用户信息成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, false));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE).
                                orElseThrow(() -> new BabyDogePawsApiException("getMe响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 采集每日奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 每日奖励信息
     */
    public Map<String, Object> pickDailyBonus(BabyDogePawsGameRequestParam param) {
        return CLIENT.sendAsync(PICK_DAILY_BONUS.build(param), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[采集每日奖励失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(param.account, response.statusCode());
                        return response.statusCode() == 401 ? pickDailyBonus(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[采集每日奖励成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("pickDailyBonus响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 获取每日奖励信息
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 每日奖励信息
     */
    public Map<String, Object> getDailyBonuses(BabyDogePawsGameRequestParam param) {
        return CLIENT.sendAsync(GET_DAILY_BONUSES.build(param), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[获取每日奖励内容失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(param.account, response.statusCode());
                        return response.statusCode() == 401 ? getDailyBonuses(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[获取每日奖励内容成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("getDailyBonuses响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 获取所有卡片
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 所有卡片
     */
    public List<Map<String, Object>> listCards(BabyDogePawsGameRequestParam param) {
        return CLIENT.sendAsync(LIST_CARDS.build(param), HttpResponse.BodyHandlers.ofString())
                .<List<Map<String, Object>>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[获取卡片列表失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(param.account, response.statusCode());
                        return response.statusCode() == 401 ? listCards(param) : new ArrayList<>();
                    } else {
                        LOGGER.info("[获取卡片列表成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, false));
                        return Https.parseJsonResponse(response, Constants.LIST_OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("listCards响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 升级卡片
     *
     * @param upgradeCard {@link UpgradeCard}
     * @return 卡片升级后的信息，其中包括用户信息和升级后的所有卡片信息
     */
    public Map<String, Object> upgradeCard(UpgradeCard upgradeCard) {
        return CLIENT.sendAsync(UPGRADE_CARD.build(upgradeCard), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[卡片升级失败]-{}:{}:{}:{}:{}", upgradeCard.account.name, upgradeCard.balance, upgradeCard.card.id(), upgradeCard.card.upgradeCost(), Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(upgradeCard.account, response.statusCode());
                        return response.statusCode() == 401 ? upgradeCard(upgradeCard) : new HashMap<>();
                    } else {
                        LOGGER.info("[卡片升级成功]-{}:{}:{}:{}:{}", upgradeCard.account.name, upgradeCard.balance, upgradeCard.card.id(), upgradeCard.card.upgradeCost(), Https.formatJsonResponse(response, false));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("cards响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 挖矿
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 挖矿后的信息
     */
    public Map<String, Object> mine(BabyDogePawsGameRequestParam param) {
        return CLIENT.sendAsync(MINE.build(param), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[挖矿失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(param.account, response.statusCode());
                        return response.statusCode() == 401 ? mine(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[挖矿成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, false));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("mine响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 获取所有任务
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 所有任务
     */
    public Map<String, Object> listChannels(BabyDogePawsGameRequestParam param) {
        return CLIENT.sendAsync(LIST_CHANNELS.build(param), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[获取任务列表失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(param.account, response.statusCode());
                        return response.statusCode() == 401 ? listChannels(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[获取任务列表成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("listChannels响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 采集任务奖励
     *
     * @param pickChannel {@link PickChannel}
     * @return 采集任务奖励响应
     */
    public Map<String, Object> pickChannel(PickChannel pickChannel) {
        return CLIENT.sendAsync(PICK_CHANNEL.build(pickChannel), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[采集任务奖励失败]-{}:{}:{}:{}", pickChannel.account.name, pickChannel.channel.id(), pickChannel.channel.inviteLink(), Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(pickChannel.account, response.statusCode());
                        return response.statusCode() == 401 ? pickChannel(pickChannel) : new HashMap<>();
                    } else {
                        LOGGER.info("[采集任务奖励成功]-{}:{}:{}:{}", pickChannel.account.name, pickChannel.channel.id(), pickChannel.channel.inviteLink(), Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("pickChannel响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 采集促销奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 采集促销奖励响应
     */
    public Map<String, Object> pickPromo(BabyDogePawsGameRequestParam param) {
        return CLIENT.sendAsync(PICK_PROMO.build(param), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[参与促销失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(param.account, response.statusCode());
                        return response.statusCode() == 401 ? pickPromo(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[参与促销成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("pickPromo响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 获取促销信息
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 响应
     */
    public Map<String, Object> getPromo(BabyDogePawsGameRequestParam param) {
        return CLIENT.sendAsync(GET_PROMO.build(param), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[获取促销信息失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(param.account, response.statusCode());
                        return response.statusCode() == 401 ? getPromo(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[获取促销信息成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("getPromo响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 获取好友列表
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 响应
     */
    public Map<String, Object> listFriends(BabyDogePawsGameRequestParam param) {
        return CLIENT.sendAsync(LIST_FRIENDS.build(param), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[获取好友列表失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        reAuthorizeIfNecessary(param.account, response.statusCode());
                        return response.statusCode() == 401 ? listFriends(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[获取好友列表成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("listFriends响应结果为空"));
                    }
                })
                .join();
    }
}
