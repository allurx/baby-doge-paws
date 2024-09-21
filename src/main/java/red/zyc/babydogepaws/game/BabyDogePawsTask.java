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
import red.zyc.babydogepaws.model.request.ResolveChannel;
import red.zyc.babydogepaws.model.request.UpgradeCard;
import red.zyc.babydogepaws.model.response.Channel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static red.zyc.kit.json.JsonOperator.JACKSON_OPERATOR;


/**
 * @author allurx
 */
@Service
public class BabyDogePawsTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePawsTask.class);
    public static volatile int mineCountMin = 1;
    public static volatile int mineCountMax = 201;

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
        scheduleAuthorize(param);
        schedulePickDailyBonus(param);
        schedulePickPromo(param);
        scheduleMine(param);
        scheduleUpgradeCard(param);
        scheduleResolveChannel(param);
    }

    /**
     * 每隔1小时授权一次，确保游戏处于活跃状态，以便能够持续产生利润
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    private void scheduleAuthorize(BabyDogePawsGameRequestParam param) {
        param.user.tasks.put("Authorize", AUTHENTICATOR.scheduleWithFixedDelay(() -> {
            try {
                babyDogePawsApi.authorize(param);
            } catch (Throwable t) {
                LOGGER.error("[执行授权task发生异常]-{}", param.user.phoneNumber, t);
            }
        }, 0L, 1L, TimeUnit.HOURS));
    }

    /**
     * 定时采集当天每日奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    private void schedulePickDailyBonus(BabyDogePawsGameRequestParam param) {
        param.user.tasks.put("PickDailyBonus", ONE_TIME_TASK_HITTER.scheduleWithFixedDelay(() -> {
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
        param.user.tasks.put("PickPromo", ONE_TIME_TASK_HITTER.scheduleWithFixedDelay(() -> {
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
            while (true) {

                // 挖矿请求
                count = ThreadLocalRandom.current().nextInt(mineCountMin, mineCountMax);
                var miningInfo = babyDogePawsApi.mine(new Mine(param.user, count));
                if (miningInfo.isEmpty()) break;

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

                // 保存本次挖矿信息
                miningInfoMapper.saveMiningInfo(param.user.id, earnPerTap, count, mined, remainingEnergy, draw);

                // 能量用完后，如果有全能量Boosts可用的话就使用
                if (remainingEnergy == 0) {
                    var fullEnergyCountBoosts = (int) babyDogePawsApi.getBoosts(param).getOrDefault("current_full_energy_count", -1);
                    if (fullEnergyCountBoosts > 0) {
                        // 下一次循环能量应该充满了
                        babyDogePawsApi.useFullEnergyBoosts(param);
                    } else {
                        break;
                    }
                }
            }
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
        param.user.tasks.put("UpgradeCard", CARD_UP_GRADER.scheduleWithFixedDelay(() -> {
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
     * 定时解决并领取任务奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    private void scheduleResolveChannel(BabyDogePawsGameRequestParam param) {
        ONE_TIME_TASK_HITTER.scheduleWithFixedDelay(() -> {
            try {
                @SuppressWarnings("unchecked")
                var channels = ((List<Map<String, Object>>) babyDogePawsApi.listChannels(param).getOrDefault("channels", new ArrayList<>()))
                        .stream().map(channel -> new Channel(
                                Long.parseLong(channel.get("id").toString()),
                                Long.parseLong(channel.get("reward").toString()),
                                (boolean) channel.get("is_resolved"),
                                (boolean) channel.get("is_reward_taken"),
                                (boolean) channel.get("is_premium")
                        )).toList();

                // 解决任务
                channels.stream()
                        .filter(channel -> !channel.isPremium() && !channel.isResolved())
                        .forEach(channel -> babyDogePawsApi.resolveChannel(new ResolveChannel(param.user, channel)));

                // 采集已解决并且没有拿过奖励的任务，任务解决后一般是1个小时可以采集奖励
                channels.stream()
                        .filter(channel -> channel.isResolved() && !channel.isRewardTaken())
                        .forEach(channel -> babyDogePawsApi.pickChannel(new ResolveChannel(param.user, channel)));
            } catch (Throwable t) {
                LOGGER.error("[执行解决任务task发生异常]-{}", param.user.phoneNumber, t);
            }
        }, 0L, 90L, TimeUnit.MINUTES);
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
                    if (canUpgradeCard(upgradeCard, Optional.ofNullable(user.maximumCardUpgradePrice).orElse(BigDecimal.valueOf(500)))) {
                        var map = babyDogePawsApi.upgradeCard(upgradeCard);
                        var latestBalance = new BigDecimal(map.getOrDefault("balance", BigDecimal.ZERO).toString());
                        var latestCards = (List<Map<String, Object>>) map.getOrDefault("cards", new ArrayList<>());
                        upgradeCard(user, latestBalance, latestCards);
                    }
                });
    }

    private boolean canUpgradeCard(UpgradeCard upgradeCard, BigDecimal maximumCardUpgradePrice) {

        UpgradeCard.UpgradeInfo upgradeInfo = upgradeCard.upgradeInfo;

        // 同时满足所有条件才能升级
        return
                // 余额比卡片升级所需的花费多
                upgradeCard.balance.compareTo(upgradeInfo.cost) >= 0

                        // 卡片升级的价格满足一定条件
                        && upgradeInfo.cost.divide(upgradeInfo.profit, 2, RoundingMode.HALF_UP).compareTo(maximumCardUpgradePrice) <= 0;


        // 卡片升级所需的花费不超过设置的阈值
        // && upgradeInfo.cost.compareTo(BigDecimal.valueOf(10000000)) <= 0;
    }

}
