package red.zyc.babydogepaws.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import red.zyc.babydogepaws.common.constant.Constants;
import red.zyc.babydogepaws.common.util.ApplicationContextHolder;
import red.zyc.babydogepaws.common.util.Https;
import red.zyc.babydogepaws.dao.UserMapper;
import red.zyc.babydogepaws.exception.BabyDogePawsApiException;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
import red.zyc.babydogepaws.model.request.Mine;
import red.zyc.babydogepaws.model.request.ResolveChannel;
import red.zyc.babydogepaws.model.request.UpgradeCard;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import static red.zyc.babydogepaws.game.BabyDogePawsApiRequest.Request.*;

/**
 * BabyDogePaws游戏http接口
 *
 * @author allurx
 */
@Service
public class BabyDogePawsApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePawsApi.class);
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30L))
            .executor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("BabyDogePawsApiRequester-", 0).factory()))
            .build();
    private static final ConcurrentHashMap<Integer, ReentrantLock> USER_LOCKS = new ConcurrentHashMap<>();
    private final UserMapper userMapper;

    public BabyDogePawsApi(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 获取每个{@link BabyDogePawsUser}的锁，
     * 同一时刻同一个用户授权方法与其它方法执行互斥，其它方法可以并行执行
     *
     * @param userId {@link BabyDogePawsUser#id}
     * @return 每个用户的锁
     */
    private static ReentrantLock getUserLock(Integer userId) {
        return USER_LOCKS.computeIfAbsent(userId, k -> new ReentrantLock());
    }

    /**
     * 如果x-api-key过期的话则需要重新授权一下
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @param code  http响应码
     * @return authorize任务有没有成功被执行
     */
    private boolean authorizeSuccess(BabyDogePawsGameRequestParam param, int code) {
        if (code == 401) {
            authorize(param);
            return !param.user.tasksCanceled;
        }
        return false;
    }

    /**
     * 同一时刻同一个用户只允许一个线程执行游戏授权<br>
     * 如果{@link BabyDogePawsUser#cancelAllTask() 登入失败次数过多导致任务被取消了}，
     * 那么就不需要执行该任务了
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 用户信息
     */
    public Map<String, Object> authorize(BabyDogePawsGameRequestParam param) {
        var userLock = getUserLock(param.user.id);
        userLock.lock();
        try {
            return param.user.tasksCanceled ? new HashMap<>() : CLIENT.sendAsync(AUTHORIZE.build(param), HttpResponse.BodyHandlers.ofString())
                    .<Map<String, Object>>thenApply(response -> {
                        if (response.statusCode() != 200) {

                            LOGGER.warn("[授权失败]-{}:{}:{}", param.user.phoneNumber, param.user.authParam, Https.formatJsonResponse(response, true));

                            // authParam过期了或者不正确，需要重新登录一下
                            if (response.statusCode() == 400) {
                                ApplicationContextHolder.getBean(BabyDogePaws.class).playBabyDogePaws(param.user, 0);
                                return new HashMap<>();
                            }

                            // 其它错误码直接返回，等待下一个定时任务执行直到游戏服务器恢复
                            return new HashMap<>();

                        } else {
                            LOGGER.info("[授权成功]-{}:{}:{}", param.user.phoneNumber, param.user.authParam, Https.formatJsonResponse(response, true));
                            return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE).map(authData -> {

                                // 更新游戏数据
                                param.user.xApiKey = String.valueOf(authData.get("access_token"));

                                // 保存或更新游戏账户的一些信息
                                var friends = listFriends(param);
                                userMapper.saveOrUpdateUser(
                                        param.user.id,
                                        Long.parseLong(authData.get("balance").toString()),
                                        (int) authData.get("profit_per_hour"),
                                        (int) authData.get("current_league"),
                                        String.valueOf(friends.get("copy_link")),
                                        param.user.xApiKey,
                                        (Integer) friends.get("friends_count"));
                                return authData;
                            }).orElseThrow(() -> new BabyDogePawsApiException("authorize响应结果为空"));
                        }
                    })
                    .join();

        } finally {
            userLock.unlock();
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
                        LOGGER.warn("[获取用户信息失败]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return authorizeSuccess(param, response.statusCode()) ? getMe(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[获取用户信息成功]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, false));
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
                        LOGGER.warn("[采集每日奖励失败]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return authorizeSuccess(param, response.statusCode()) ? pickDailyBonus(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[采集每日奖励成功]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
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
                        LOGGER.warn("[获取每日奖励内容失败]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return authorizeSuccess(param, response.statusCode()) ? getDailyBonuses(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[获取每日奖励内容成功]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
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
                        LOGGER.warn("[获取卡片列表失败]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return authorizeSuccess(param, response.statusCode()) ? listCards(param) : new ArrayList<>();
                    } else {
                        LOGGER.info("[获取卡片列表成功]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, false));
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
                        LOGGER.warn("[卡片升级失败]-{}:{}:{}:{}:{}", upgradeCard.user.phoneNumber, upgradeCard.balance, upgradeCard.card.cardId, upgradeCard.upgradeInfo.cost, Https.formatJsonResponse(response, true));
                        return authorizeSuccess(upgradeCard, response.statusCode()) ? upgradeCard(upgradeCard) : new HashMap<>();
                    } else {
                        LOGGER.info("[卡片升级成功]-{}:{}:{}:{}:{}", upgradeCard.user.phoneNumber, upgradeCard.balance, upgradeCard.card.cardId, upgradeCard.upgradeInfo.cost, Https.formatJsonResponse(response, false));
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
    public Map<String, Object> mine(Mine param) {
        return CLIENT.sendAsync(MINE.build(param), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[挖矿失败]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return authorizeSuccess(param, response.statusCode()) ? mine(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[挖矿成功]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
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
        return CLIENT.sendAsync(LIST_CHANNEL.build(param), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[获取任务列表失败]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return authorizeSuccess(param, response.statusCode()) ? listChannels(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[获取任务列表成功]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("listChannels响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 解决任务
     *
     * @param resolveChannel {@link ResolveChannel}
     * @return 响应
     */
    public Map<String, Object> resolveChannel(ResolveChannel resolveChannel) {
        return CLIENT.sendAsync(RESOLVE_CHANNEL.build(resolveChannel), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[解决任务失败]-{}:{}:{}", resolveChannel.user.phoneNumber, resolveChannel.channel.id(), Https.formatJsonResponse(response, true));
                        return authorizeSuccess(resolveChannel, response.statusCode()) ? resolveChannel(resolveChannel) : new HashMap<>();
                    } else {
                        LOGGER.info("[解决任务成功]-{}:{}:{}", resolveChannel.user.phoneNumber, resolveChannel.channel.id(), Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("resolveChannel响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 采集任务
     *
     * @param resolveChannel {@link ResolveChannel}
     * @return 响应
     */
    public Map<String, Object> pickChannel(ResolveChannel resolveChannel) {
        return CLIENT.sendAsync(PICK_CHANNEL.build(resolveChannel), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[采集任务失败]-{}:{}:{}", resolveChannel.user.phoneNumber, resolveChannel.channel.id(), Https.formatJsonResponse(response, true));
                        return authorizeSuccess(resolveChannel, response.statusCode()) ? pickChannel(resolveChannel) : new HashMap<>();
                    } else {
                        LOGGER.info("[采集任务成功]-{}:{}:{}", resolveChannel.user.phoneNumber, resolveChannel.channel.id(), Https.formatJsonResponse(response, true));
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
                        LOGGER.warn("[参与促销失败]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return authorizeSuccess(param, response.statusCode()) ? pickPromo(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[参与促销成功]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
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
                        LOGGER.warn("[获取促销信息失败]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return authorizeSuccess(param, response.statusCode()) ? getPromo(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[获取促销信息成功]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
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
                        LOGGER.warn("[获取好友列表失败]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return authorizeSuccess(param, response.statusCode()) ? listFriends(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[获取好友列表成功]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, false));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("listFriends响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 获取激励信息
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 响应
     */
    public Map<String, Object> getBoosts(BabyDogePawsGameRequestParam param) {
        return CLIENT.sendAsync(GET_BOOSTS.build(param), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[获取激励信息失败]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return authorizeSuccess(param, response.statusCode()) ? getBoosts(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[获取激励信息成功]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("boosts响应结果为空"));
                    }
                })
                .join();
    }

    /**
     * 使用全能量激励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 响应
     */
    public Map<String, Object> useFullEnergyBoosts(BabyDogePawsGameRequestParam param) {
        return CLIENT.sendAsync(USE_FULL_ENERGY_BOOSTS.build(param), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.warn("[使用全能量激励失败]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return authorizeSuccess(param, response.statusCode()) ? useFullEnergyBoosts(param) : new HashMap<>();
                    } else {
                        LOGGER.info("[使用全能量激励成功]-{}:{}", param.user.phoneNumber, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                                .orElseThrow(() -> new BabyDogePawsApiException("boosts响应结果为空"));
                    }
                })
                .join();
    }
}
