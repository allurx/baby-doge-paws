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
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static red.zyc.babydogepaws.common.constant.BabyDogePawsGame.Request.*;

/**
 * @author allurx
 */
@Service
public class BabyDogePawsApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePawsApi.class);
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30L)).build();
    private static final ConcurrentHashMap<Integer, ReentrantReadWriteLock> LOCKS = new ConcurrentHashMap<>();

    /**
     * 获取每个{@link BabyDogePawsUser}的锁，如果不存在则创建新锁
     *
     * @param userId {@link BabyDogePawsUser#id}
     * @return {@link BabyDogePawsUser}的锁
     */
    private static ReentrantReadWriteLock getLock(Integer userId) {
        return LOCKS.computeIfAbsent(userId, k -> new ReentrantReadWriteLock());
    }

    /**
     * 如果x-api-key过期的话则需要重新授权一下，参考{@link ReentrantReadWriteLock}注释中的CachedData例子
     *
     * @param account {@link BabyDogePawsAccount}
     * @param code    http响应码
     */
    private void reAuthorizeIfNecessary(BabyDogePawsAccount account, int code, ReentrantReadWriteLock lock) {
        if (code == 401) {

            account.dataValid = false;

            if (!account.dataValid) {

                // 获取写锁前必须先释放读锁（读锁无法升级，但是写锁可以降级）
                lock.readLock().unlock();

                // 因为这个方法必定是被拥有读锁的线程执行的，
                // 所以这行代码执行前都会存在多个线程执行的状态
                lock.writeLock().lock();
                try {
                    // 其它线程可能已经修改了dataValid
                    if (!account.dataValid) {
                        authorize(new BabyDogePawsGameRequestParam(account));
                    }
                    // 锁降级
                    lock.readLock().lock();
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }
    }

    /**
     * 游戏授权
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 用户信息
     */
    public Map<String, Object> authorize(BabyDogePawsGameRequestParam param) {
        var account = param.account;
        return CLIENT.sendAsync(AUTHORIZE.build(param), HttpResponse.BodyHandlers.ofString())
                .<Map<String, Object>>thenApply((response) -> {
                    if (response.statusCode() != 200) {

                        LOGGER.error("[授权失败]-{}:{}:{}", account.name, account.user.authParam, Https.formatJsonResponse(response, true));

                        // authParam过期了，需要重新登录一下
                        if (response.statusCode() == 400) {
                            ApplicationContextHolder.getBean(BabyDogePaws.class).playBabyDogePaws(account, 0);
                            return new HashMap<>();

                            // 服务器维护了，直接返回，等待下一个定时任务执行直到服务器恢复
                        } else if (response.statusCode() == 503) {
                            return new HashMap<>();
                        } else {
                            throw new BabyDogePawsException("授权失败，未知的http响应码: " + Https.formatJsonResponse(response, true));
                        }
                    } else {
                        LOGGER.info("[授权成功]-{}:{}:{}", account.name, account.user.authParam, Https.formatJsonResponse(response, true));
                        return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE).map((authData) -> {
                            account.data = authData;
                            account.dataValid = true;
                            return authData;
                        }).orElseThrow(() -> new BabyDogePawsApiException("authorize响应结果为空"));
                    }
                }).join();

    }

    /**
     * 获取用户信息
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 用户信息
     */
    public Map<String, Object> getMe(BabyDogePawsGameRequestParam param) {
        var lock = getLock(param.account.user.id);
        lock.readLock().lock();
        try {
            var response = CLIENT.send(GET_ME.build(param), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.error("[获取用户信息失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                reAuthorizeIfNecessary(param.account, response.statusCode(), lock);
                return response.statusCode() == 401 ? getMe(param) : new HashMap<>();
            } else {
                LOGGER.info("[获取用户信息成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, false));
                return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE).
                        orElseThrow(() -> new BabyDogePawsApiException("getMe响应结果为空"));
            }
        } catch (Throwable t) {
            LOGGER.error("[获取用户信息失败]-{}:", param.account.name, t);
            return new HashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 采集每日奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 每日奖励信息
     */
    public Map<String, Object> pickDailyBonus(BabyDogePawsGameRequestParam param) {
        var lock = getLock(param.account.user.id);
        lock.readLock().lock();
        try {
            var response = CLIENT.send(PICK_DAILY_BONUS.build(param), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.error("[采集每日奖励失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                reAuthorizeIfNecessary(param.account, response.statusCode(), lock);
                Mails.sendTextMail(param.account.name + "采集每日奖励失败", Https.formatJsonResponse(response, true));
                return response.statusCode() == 401 ? pickDailyBonus(param) : new HashMap<>();
            } else {
                LOGGER.info("[采集每日奖励成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("pickDailyBonus响应结果为空"));
            }
        } catch (Throwable t) {
            LOGGER.error("[采集每日奖励失败]-{}:", param.account.name, t);
            return new HashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取每日奖励信息
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 每日奖励信息
     */
    public Map<String, Object> getDailyBonuses(BabyDogePawsGameRequestParam param) {
        var lock = getLock(param.account.user.id);
        lock.readLock().lock();
        try {
            var response = CLIENT.send(GET_DAILY_BONUSES.build(param), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.error("[获取每日奖励内容失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                reAuthorizeIfNecessary(param.account, response.statusCode(), lock);
                return response.statusCode() == 401 ? getDailyBonuses(param) : new HashMap<>();
            } else {
                LOGGER.info("[获取每日奖励内容成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("getDailyBonuses响应结果为空"));
            }
        } catch (Throwable t) {
            LOGGER.error("[获取每日奖励内容失败]-{}:", param.account.name, t);
            return new HashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取所有卡片
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 所有卡片
     */
    public List<Map<String, Object>> listCards(BabyDogePawsGameRequestParam param) {
        var lock = getLock(param.account.user.id);
        lock.readLock().lock();
        try {
            var response = CLIENT.send(LIST_CARDS.build(param), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.error("[获取卡片列表失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                reAuthorizeIfNecessary(param.account, response.statusCode(), lock);
                return response.statusCode() == 401 ? listCards(param) : new ArrayList<>();
            } else {
                LOGGER.info("[获取卡片列表成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, false));
                return Https.parseJsonResponse(response, Constants.LIST_OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("listCards响应结果为空"));
            }
        } catch (Throwable t) {
            LOGGER.error("[获取卡片列表失败]-{}:", param.account.name, t);
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 升级卡片
     *
     * @param upgradeCard {@link UpgradeCard}
     * @return 卡片升级后的信息，其中包括用户信息和升级后的所有卡片信息
     */
    public Map<String, Object> upgradeCard(UpgradeCard upgradeCard) {
        var lock = getLock(upgradeCard.account.user.id);
        lock.readLock().lock();
        try {
            var response = CLIENT.send(UPGRADE_CARD.build(upgradeCard), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.error("[卡片升级失败]-{}:{}:{}:{}:{}", upgradeCard.account.name, upgradeCard.balance, upgradeCard.card.id(), upgradeCard.card.upgradeCost(), Https.formatJsonResponse(response, true));
                reAuthorizeIfNecessary(upgradeCard.account, response.statusCode(), lock);
                return response.statusCode() == 401 ? upgradeCard(upgradeCard) : new HashMap<>();
            } else {
                LOGGER.info("[卡片升级成功]-{}:{}:{}:{}:{}", upgradeCard.account.name, upgradeCard.balance, upgradeCard.card.id(), upgradeCard.card.upgradeCost(), Https.formatJsonResponse(response, false));
                return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("cards响应结果为空"));
            }
        } catch (Throwable t) {
            LOGGER.error("[卡片升级失败]-{}:", upgradeCard.account.name, t);
            return new HashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 挖矿
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 挖矿后的信息
     */
    public Map<String, Object> mine(BabyDogePawsGameRequestParam param) {
        var lock = getLock(param.account.user.id);
        lock.readLock().lock();
        try {
            var response = CLIENT.send(MINE.build(param), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.error("[挖矿失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                reAuthorizeIfNecessary(param.account, response.statusCode(), lock);
                return response.statusCode() == 401 ? mine(param) : new HashMap<>();
            } else {
                LOGGER.info("[挖矿成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, false));
                return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("mine响应结果为空"));
            }
        } catch (Throwable t) {
            LOGGER.error("[挖矿失败]-{}:", param.account.name, t);
            return new HashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取所有任务
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 所有任务
     */
    public Map<String, Object> listChannels(BabyDogePawsGameRequestParam param) {
        var lock = getLock(param.account.user.id);
        lock.readLock().lock();
        try {
            var response = CLIENT.send(LIST_CHANNELS.build(param), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.error("[获取任务列表失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                reAuthorizeIfNecessary(param.account, response.statusCode(), lock);
                return response.statusCode() == 401 ? listChannels(param) : new HashMap<>();
            } else {
                LOGGER.info("[获取任务列表成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("listChannels响应结果为空"));
            }
        } catch (Throwable t) {
            LOGGER.error("[获取任务列表失败]-{}:", param.account.name, t);
            return new HashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 采集任务奖励
     *
     * @param pickChannel {@link PickChannel}
     * @return 采集任务奖励响应
     */
    public Map<String, Object> pickChannel(PickChannel pickChannel) {
        var lock = getLock(pickChannel.account.user.id);
        lock.readLock().lock();
        try {
            var response = CLIENT.send(PICK_CHANNEL.build(pickChannel), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.error("[采集任务奖励失败]-{}:{}:{}:{}", pickChannel.account.name, pickChannel.channel.id(), pickChannel.channel.inviteLink(), Https.formatJsonResponse(response, true));
                reAuthorizeIfNecessary(pickChannel.account, response.statusCode(), lock);
                return response.statusCode() == 401 ? pickChannel(pickChannel) : new HashMap<>();
            } else {
                LOGGER.info("[采集任务奖励成功]-{}:{}:{}:{}", pickChannel.account.name, pickChannel.channel.id(), pickChannel.channel.inviteLink(), Https.formatJsonResponse(response, true));
                return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("pickChannel响应结果为空"));
            }
        } catch (Throwable t) {
            LOGGER.error("[采集任务奖励失败]-{}:", pickChannel.account.name, t);
            return new HashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 采集促销奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 采集促销奖励响应
     */
    public Map<String, Object> pickPromo(BabyDogePawsGameRequestParam param) {
        var lock = getLock(param.account.user.id);
        lock.readLock().lock();
        try {
            var response = CLIENT.send(PICK_PROMO.build(param), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.error("[参与促销失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                reAuthorizeIfNecessary(param.account, response.statusCode(), lock);
                Mails.sendTextMail(param.account.name + "参与促销失败", Https.formatJsonResponse(response, true));
                return response.statusCode() == 401 ? pickPromo(param) : new HashMap<>();
            } else {
                LOGGER.info("[参与促销成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("pickPromo响应结果为空"));
            }
        } catch (Throwable t) {
            LOGGER.error("[参与促销失败]-{}:", param.account.name, t);
            return new HashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取促销信息
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 响应
     */
    public Map<String, Object> getPromo(BabyDogePawsGameRequestParam param) {
        var lock = getLock(param.account.user.id);
        lock.readLock().lock();
        try {
            var response = CLIENT.send(GET_PROMO.build(param), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.error("[获取促销信息失败]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                reAuthorizeIfNecessary(param.account, response.statusCode(), lock);
                return response.statusCode() == 401 ? getPromo(param) : new HashMap<>();
            } else {
                LOGGER.info("[获取促销信息成功]-{}:{}", param.account.name, Https.formatJsonResponse(response, true));
                return Https.parseJsonResponse(response, Constants.OBJECT_DATA_TYPE)
                        .orElseThrow(() -> new BabyDogePawsApiException("getPromo响应结果为空"));
            }
        } catch (Throwable t) {
            LOGGER.error("[获取促销信息失败]-{}:", param.account.name, t);
            return new HashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }
}
