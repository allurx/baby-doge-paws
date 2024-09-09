package red.zyc.babydogepaws.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import red.zyc.babydogepaws.common.NamedThreadFactory;
import red.zyc.babydogepaws.dao.CardMapper;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.babydogepaws.model.persistent.Card;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
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
import java.util.concurrent.TimeUnit;

import static red.zyc.kit.json.JsonOperator.JACKSON_OPERATOR;


/**
 * @author allurx
 */
@Service
public class BabyDogePawsTask {

    public static volatile double limit = 1517.26;

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePawsTask.class);
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();

    private static final ScheduledThreadPoolExecutor AUTHENTICATOR = new ScheduledThreadPoolExecutor(0, new NamedThreadFactory("Authenticator", true));
    private static final ScheduledThreadPoolExecutor MINER = new ScheduledThreadPoolExecutor(0, new NamedThreadFactory("Miner", true));
    private static final ScheduledThreadPoolExecutor CARD_UP_GRADER = new ScheduledThreadPoolExecutor(0, new NamedThreadFactory("CardUpGrader", true));
    private static final ScheduledThreadPoolExecutor ONE_TIME_TASK_HITTER = new ScheduledThreadPoolExecutor(0, new NamedThreadFactory("OneTimeMissionHitter", true));
    private final BabyDogePawsApi babyDogePawsApi;
    private final CardMapper cardMapper;

    @Value("${baby-doge-paws.card-upgrade-info-tracker}")
    private List<String> trackers;

    public BabyDogePawsTask(BabyDogePawsApi babyDogePawsApi, CardMapper cardMapper) {
        this.babyDogePawsApi = babyDogePawsApi;
        this.cardMapper = cardMapper;
    }

    /**
     * 启动所有定时任务
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    public void schedule(BabyDogePawsGameRequestParam param) {
        param.user.tasks.add(scheduleAuthorize(param));
        param.user.tasks.add(schedulePickDailyBonus(param));
        param.user.tasks.add(schedulePickPromo(param));
        param.user.tasks.add(scheduleMine(param));
        param.user.tasks.add(scheduleUpgradeCard(param));
    }

    /**
     * 每隔1小时授权一次，确保游戏处于活跃状态，以便能够持续产生利润
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 任务
     */
    private ScheduledFuture<?> scheduleAuthorize(BabyDogePawsGameRequestParam param) {
        return AUTHENTICATOR.scheduleAtFixedRate(() -> {
            try {
                babyDogePawsApi.authorize(param);
            } catch (Throwable t) {
                LOGGER.error("[执行授权task发生异常]-{}", param.user.phoneNumber, t);
            }
        }, 0L, 1L, TimeUnit.HOURS);
    }

    /**
     * 定时采集当天每日奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 任务
     */
    private ScheduledFuture<?> schedulePickDailyBonus(BabyDogePawsGameRequestParam param) {
        return ONE_TIME_TASK_HITTER.scheduleAtFixedRate(() -> {
            try {
                babyDogePawsApi.pickDailyBonus(param);
            } catch (Throwable t) {
                LOGGER.error("[执行采集每日奖励task发生异常]-{}", param.user.phoneNumber, t);
            }
        }, 0L, 1L, TimeUnit.HOURS);
    }

    /**
     * 定时采集促销奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 任务
     */
    private ScheduledFuture<?> schedulePickPromo(BabyDogePawsGameRequestParam param) {
        return ONE_TIME_TASK_HITTER.scheduleAtFixedRate(() -> {
            try {
                babyDogePawsApi.pickPromo(param);
            } catch (Throwable t) {
                LOGGER.error("[执行采集促销奖励task发生异常]-{}", param.user.phoneNumber, t);
            }
        }, 0L, 1L, TimeUnit.HOURS);
    }

    /**
     * 定时挖矿
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 任务
     */
    private ScheduledFuture<?> scheduleMine(BabyDogePawsGameRequestParam param) {
        return MINER.scheduleAtFixedRate(() -> {
            try {
                babyDogePawsApi.mine(param);
            } catch (Throwable t) {
                LOGGER.error("[执行挖矿task发生异常]-{}", param.user.phoneNumber, t);
            }
        }, 0L, BabyDogePawsUser.MINE_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * 定时升级卡片
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     * @return 任务
     */
    private ScheduledFuture<?> scheduleUpgradeCard(BabyDogePawsGameRequestParam param) {
        return CARD_UP_GRADER.scheduleAtFixedRate(() -> {
            try {
                var balance = new BigDecimal(babyDogePawsApi.getMe(param).getOrDefault("balance", BigDecimal.ZERO).toString());
                var cards = babyDogePawsApi.listCards(param);
                upgradeCard(param.user, balance, cards);
            } catch (Throwable t) {
                LOGGER.error("[执行升级卡片task发生异常]-{}", param.user.phoneNumber, t);
            }
        }, 0L, 3L, TimeUnit.MINUTES);
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
                .peek(upgradeCard -> trackers.stream()
                        .filter(s -> s.equals(user.phoneNumber))
                        .forEach(s -> cardMapper.saveOrUpdateCard(upgradeCard.card, upgradeCard.upgradeInfo)))

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
