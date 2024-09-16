package red.zyc.babydogepaws.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import red.zyc.babydogepaws.config.BabyDogePawsProperties;
import red.zyc.babydogepaws.dao.CardMapper;
import red.zyc.babydogepaws.dao.MiningInfoMapper;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.babydogepaws.model.persistent.Card;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
import red.zyc.babydogepaws.model.request.Mine;
import red.zyc.babydogepaws.model.request.PickChannel;
import red.zyc.babydogepaws.model.request.UpgradeCard;
import red.zyc.babydogepaws.model.response.Channel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static red.zyc.kit.json.JsonOperator.JACKSON_OPERATOR;


/**
 * @author allurx
 */
@Service
public class BabyDogePawsTask {

    public static volatile double limit = 500;
    public static volatile int mineCountMin = 1;
    public static volatile int mineCountMax = 201;

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePawsTask.class);
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();

    private static final ScheduledThreadPoolExecutor AUTHENTICATOR = new ScheduledThreadPoolExecutor(0, Thread.ofVirtual().name("Authenticator-", 0).factory());
    private static final ScheduledThreadPoolExecutor MINER = new ScheduledThreadPoolExecutor(0, Thread.ofVirtual().name("Miner-", 0).factory());
    private static final ScheduledThreadPoolExecutor CARD_UP_GRADER = new ScheduledThreadPoolExecutor(0, Thread.ofVirtual().name("CardUpGrader-", 0).factory());
    private static final ScheduledThreadPoolExecutor ONE_TIME_TASK_HITTER = new ScheduledThreadPoolExecutor(0, Thread.ofVirtual().name("OneTimeMissionHitter-", 0).factory());
    private final BabyDogePawsApi babyDogePawsApi;
    private final CardMapper cardMapper;
    private final MiningInfoMapper miningInfoMapper;
    private final BabyDogePawsProperties babyDogePawsProperties;


    public BabyDogePawsTask(BabyDogePawsApi babyDogePawsApi, CardMapper cardMapper, MiningInfoMapper miningInfoMapper, BabyDogePawsProperties babyDogePawsProperties) {
        this.babyDogePawsApi = babyDogePawsApi;
        this.cardMapper = cardMapper;
        this.miningInfoMapper = miningInfoMapper;
        this.babyDogePawsProperties = babyDogePawsProperties;
    }

    /**
     * 启动所有定时任务
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    public void schedule(BabyDogePawsGameRequestParam param) {
        schedulePickDailyBonus(param);
        schedulePickPromo(param);
        scheduleMine(param);
        scheduleUpgradeCard(param);
    }

    /**
     * 定时采集当天每日奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    private void schedulePickDailyBonus(BabyDogePawsGameRequestParam param) {
        param.user.tasks.put("PickDailyBonus", ONE_TIME_TASK_HITTER.scheduleAtFixedRate(() -> {
            try {
                babyDogePawsApi.pickDailyBonus(param);
            } catch (Throwable t) {
                LOGGER.error("[执行采集每日奖励task发生异常]-{}", param.user.phoneNumber, t);
            }
        }, 0L, 1L, TimeUnit.HOURS));
    }

    /**
     * 定时采集促销奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    private void schedulePickPromo(BabyDogePawsGameRequestParam param) {
        param.user.tasks.put("PickPromo", ONE_TIME_TASK_HITTER.scheduleAtFixedRate(() -> {
            try {
                babyDogePawsApi.pickPromo(param);
            } catch (Throwable t) {
                LOGGER.error("[执行采集促销奖励task发生异常]-{}", param.user.phoneNumber, t);
            }
        }, 0L, 1L, TimeUnit.HOURS));
    }

    /**
     * 定时挖矿
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    private void scheduleMine(BabyDogePawsGameRequestParam param) {
        int timeToFullyCharge = 0;
        try {
            int remainingEnergy;
            int maxEnergy;
            int earnPerTap;
            int count;
            int mined;
            String draw;
            do {

                // 挖矿请求
                count = ThreadLocalRandom.current().nextInt(mineCountMin, mineCountMax);
                var miningInfo = babyDogePawsApi.mine(new Mine(param.user, count));

                // 挖矿请求返回的用户信息
                @SuppressWarnings("unchecked")
                var userInfo = (Map<String, Object>) miningInfo.get("user");
                remainingEnergy = (int) userInfo.getOrDefault("energy", 0);
                maxEnergy = (int) userInfo.getOrDefault("max_energy", 0);
                earnPerTap = (int) userInfo.getOrDefault("earn_per_tap", 0);

                // 挖矿请求返回的挖矿信息
                @SuppressWarnings("unchecked")
                var mineValue = (Map<String, Object>) miningInfo.get("mine");
                mined = (int) mineValue.getOrDefault("mined", 0);

                // 挖矿请求返回的奖励信息
                var drawValue = miningInfo.get("draw");
                draw = JACKSON_OPERATOR.toJsonString(drawValue == null ? Map.of() : drawValue);

                // 能量充满所需时间（秒）
                timeToFullyCharge = Math.ceilDiv(maxEnergy, 3);

                miningInfoMapper.saveMiningInfo(param.user.id, earnPerTap, count, mined, remainingEnergy, draw);

            } while (remainingEnergy != 0);
        } catch (Throwable t) {
            LOGGER.error("[执行挖矿task发生异常]-{}", param.user.phoneNumber, t);
        } finally {
            param.user.tasks.put("Mine",
                    MINER.schedule(() -> scheduleMine(param),
                            // 游戏服务器宕机的时候，这个值为0，此时设置一个固定延迟调度直到服务器恢复
                            timeToFullyCharge == 0 ? 60 : timeToFullyCharge,
                            TimeUnit.SECONDS)
            );
        }
    }

    /**
     * 定时升级卡片
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    private void scheduleUpgradeCard(BabyDogePawsGameRequestParam param) {
        param.user.tasks.put("UpgradeCard", CARD_UP_GRADER.scheduleAtFixedRate(() -> {
            try {
                var balance = new BigDecimal(babyDogePawsApi.getMe(param).getOrDefault("balance", BigDecimal.ZERO).toString());
                var cards = babyDogePawsApi.listCards(param);
                upgradeCard(param.user, balance, cards);
            } catch (Throwable t) {
                LOGGER.error("[执行升级卡片task发生异常]-{}", param.user.phoneNumber, t);
            }
        }, 0L, 3L, TimeUnit.MINUTES));
    }

    /**
     * 定时采集任务奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 任务
     */
    private ScheduledFuture<?> schedulePickChannel(BabyDogePawsGameRequestParam param) {
        return ONE_TIME_TASK_HITTER.scheduleAtFixedRate(() -> {
            try {
                @SuppressWarnings("unchecked")
                var channels = (List<Map<String, Object>>) babyDogePawsApi.listChannels(param).getOrDefault("channels", new ArrayList<>());
                channels.stream().parallel()
                        .map(channel -> new Channel(channel.get("id").toString(), channel.get("invite_link").toString(), (boolean) channel.get("is_available")))
                        .filter(Channel::isAvailable)
                        .forEach(channel -> inviteLink(channel.inviteLink())
                                .ifPresent(uri -> CLIENT.sendAsync(HttpRequest.newBuilder().uri(uri).GET().build(), HttpResponse.BodyHandlers.discarding()).thenAccept(response -> {
                                    LOGGER.info("[点击邀请链接响应成功]:{}", channel.inviteLink());
                                    babyDogePawsApi.pickChannel(new PickChannel(param.user, channel));
                                }).exceptionally(t -> {
                                    LOGGER.error("[点击邀请链接响应失败]:{}", channel.inviteLink(), t);
                                    babyDogePawsApi.pickChannel(new PickChannel(param.user, channel));
                                    return null;
                                }).join()));
            } catch (Throwable t) {
                LOGGER.error("[执行采集任务奖励task发生异常]-{}", param.user.phoneNumber, t);
            }
        }, 0L, 1L, TimeUnit.HOURS);
    }

    /**
     * 升级卡片
     *
     * @param balance 当前余额
     * @param cards   卡片列表
     */
    @SuppressWarnings("unchecked")
    private void upgradeCard(BabyDogePawsUser user, BigDecimal balance, List<Map<String, Object>> cards) {
        cards.stream()
                .flatMap(o -> ((List<Map<String, Object>>) o.get("cards")).stream().peek(card -> card.put("category_name", o.get("name"))))
                .map(cardMap -> new UpgradeCard(
                        user,
                        balance,
                        JACKSON_OPERATOR.copyProperties(cardMap, Card.class),
                        JACKSON_OPERATOR.copyProperties(cardMap, UpgradeCard.UpgradeInfo.class),
                        (Boolean) cardMap.get("is_available")))

                // 用新号来追踪卡片升级信息
                .peek(upgradeCard -> {
                    if (babyDogePawsProperties.cardUpgradeInfoTracker().contains(user.phoneNumber)) {
                        cardMapper.saveOrUpdateCard(upgradeCard.card, upgradeCard.upgradeInfo);
                    }
                })

                // 注意：available变成true后，之前存在的requirement就会变成null
                .filter(upgradeCard -> upgradeCard.available)

                // 先按照价格升序，再按照花费升序，
                // 尽快增加pph（升级pph优先）
                .min(Comparator.<UpgradeCard, BigDecimal>comparing(upgradeCard -> upgradeCard.upgradeInfo.cost.divide(upgradeCard.upgradeInfo.profit, 2, RoundingMode.HALF_UP))
                        .thenComparing(upgradeCard -> upgradeCard.upgradeInfo.cost))
                .ifPresent(upgradeCard -> {
                    if (canUpgradeCard(upgradeCard)) {
                        var map = babyDogePawsApi.upgradeCard(upgradeCard);
                        var latestBalance = new BigDecimal(map.getOrDefault("balance", BigDecimal.ZERO).toString());
                        var latestCards = (List<Map<String, Object>>) map.getOrDefault("cards", new ArrayList<>());
                        upgradeCard(user, latestBalance, latestCards);
                    }
                });
    }

    private boolean canUpgradeCard(UpgradeCard upgradeCard) {

        UpgradeCard.UpgradeInfo upgradeInfo = upgradeCard.upgradeInfo;

        // 同时满足所有条件才能升级
        return
                // 余额比卡片升级所需的花费多
                upgradeCard.balance.compareTo(upgradeInfo.cost) >= 0

                        // 卡片升级的价格满足一定条件
                        && upgradeInfo.cost.divide(upgradeInfo.profit, 2, RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(limit)) < 0;


        // 卡片升级所需的花费不超过设置的阈值
        // && upgradeInfo.cost.compareTo(BigDecimal.valueOf(10000000)) <= 0;
    }

    private Optional<URI> inviteLink(String link) {
        try {
            var uri = new URI(link);
            var scheme = uri.getScheme();
            if (scheme != null && (scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null) {
                return Optional.of(uri);
            } else {
                LOGGER.warn("[无效的邀请链接]:{}", link);
                return Optional.empty();
            }
        } catch (Throwable t) {
            LOGGER.error("[无效的邀请链接]:{}", link, t);
            return Optional.empty();
        }
    }
}
