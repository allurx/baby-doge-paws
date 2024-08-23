package red.zyc.babydogepaws.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import red.zyc.babydogepaws.common.NamedThreadFactory;
import red.zyc.babydogepaws.model.BabyDogePawsAccount;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
import red.zyc.babydogepaws.model.request.PickChannel;
import red.zyc.babydogepaws.model.request.UpgradeCard;
import red.zyc.babydogepaws.model.response.Card;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static red.zyc.babydogepaws.common.util.Commons.safeRunnable;

/**
 * @author allurx
 */
@Service
public class BabyDogePawsTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePawsTask.class);
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();

    private static final ScheduledExecutorService BABY_DOGE_PAWS_AUTH = new ScheduledThreadPoolExecutor(0, new NamedThreadFactory("BabyDogePawsAuth", true));
    private static final ScheduledExecutorService BABY_DOGE_PAWS_MINER = new ScheduledThreadPoolExecutor(0, new NamedThreadFactory("BabyDogePawsMiner", true));
    private static final ScheduledExecutorService BABY_DOGE_PAWS_UPGRADE_CARD = new ScheduledThreadPoolExecutor(0, new NamedThreadFactory("BabyDogePawsUpgradeCard", true));
    private static final ScheduledExecutorService BABY_DOGE_PAWS_SUCCESS_ONCE_TASK = new ScheduledThreadPoolExecutor(0, new NamedThreadFactory("BabyDogePawsSuccessOnceTask", true));
    private static final List<String> IGNORED_USER = List.of("86-19962006575", "1-8502950634");
    private final BabyDogePawsApi babyDogePawsApi;

    public BabyDogePawsTask(BabyDogePawsApi babyDogePawsApi) {
        this.babyDogePawsApi = babyDogePawsApi;
    }

    /**
     * 调度所有定时任务
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    public void schedule(BabyDogePawsGameRequestParam param) {

        // 初始化用户数据，因为有些定时任务需要使用到用户数据，所以这里最好提前初始化一下，虽然影响不大
        babyDogePawsApi.authorize(param);

        // 启动所有定时任务
        scheduleAuthorize(param);
        schedulePickDailyBonus(param);
        schedulePickPromo(param);
        scheduleMine(param);
        scheduleUpgradeCard(param);
    }

    /**
     * 每隔1小时授权一次，确保游戏处于活跃状态，以便能够持续产生利润
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    private void scheduleAuthorize(BabyDogePawsGameRequestParam param) {
        BABY_DOGE_PAWS_AUTH.scheduleAtFixedRate(safeRunnable(() -> babyDogePawsApi.authorize(param)), 1L, 1L, TimeUnit.HOURS);
    }

    /**
     * 定时采集当天每日奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    private void schedulePickDailyBonus(BabyDogePawsGameRequestParam param) {
        BABY_DOGE_PAWS_SUCCESS_ONCE_TASK.scheduleAtFixedRate(safeRunnable(() -> {
            if ((boolean) babyDogePawsApi.getDailyBonuses(param).getOrDefault("has_available", false)) {
                babyDogePawsApi.pickDailyBonus(param);
            }
        }), 0L, 1L, TimeUnit.HOURS);
    }

    /**
     * 定时采集促销奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    private void schedulePickPromo(BabyDogePawsGameRequestParam param) {
        BABY_DOGE_PAWS_SUCCESS_ONCE_TASK.scheduleAtFixedRate(safeRunnable(() -> {
            if (!(boolean) babyDogePawsApi.getPromo(param).getOrDefault("is_reward_taken", true)) {
                babyDogePawsApi.pickPromo(param);
            }
        }), 0L, 1L, TimeUnit.HOURS);
    }

    /**
     * 定时挖矿
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    private void scheduleMine(BabyDogePawsGameRequestParam param) {
        BABY_DOGE_PAWS_MINER.scheduleAtFixedRate(safeRunnable(() -> babyDogePawsApi.mine(param)),
                0L, param.account.mineInterval(), TimeUnit.SECONDS);
    }

    /**
     * 定时升级卡片
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    private void scheduleUpgradeCard(BabyDogePawsGameRequestParam param) {
        BABY_DOGE_PAWS_UPGRADE_CARD.scheduleAtFixedRate(safeRunnable(() -> {
            BigDecimal balance = new BigDecimal(babyDogePawsApi.getMe(param).getOrDefault("balance", BigDecimal.ZERO).toString());
            List<Map<String, Object>> cards = babyDogePawsApi.listCards(param);
            upgradeCard(param.account, balance, cards);
        }), 0L, 3L, TimeUnit.MINUTES);
    }

    /**
     * 定时采集任务奖励
     *
     * @param param {@link BabyDogePawsGameRequestParam}
     */
    public void schedulePickChannel(BabyDogePawsGameRequestParam param) {
        BABY_DOGE_PAWS_SUCCESS_ONCE_TASK.scheduleAtFixedRate(safeRunnable(() -> {
            @SuppressWarnings("unchecked")
            var channels = (List<Map<String, Object>>) babyDogePawsApi.listChannels(param).getOrDefault("channels", new ArrayList<>());
            channels.stream().parallel()
                    .map(channel -> new Channel(channel.get("id").toString(), channel.get("invite_link").toString(), (boolean) channel.get("is_available")))
                    .filter(Channel::isAvailable)
                    .forEach(channel -> inviteLink(channel.inviteLink())
                            .ifPresent((uri) -> CLIENT.sendAsync(HttpRequest.newBuilder().uri(uri).GET().build(), HttpResponse.BodyHandlers.discarding()).thenAccept(response -> {
                                LOGGER.info("[点击邀请链接响应成功]:{}", channel.inviteLink());
                                babyDogePawsApi.pickChannel(new PickChannel(param.account, channel));
                            }).exceptionally(t -> {
                                LOGGER.error("[点击邀请链接响应失败]:{}", channel.inviteLink(), t);
                                babyDogePawsApi.pickChannel(new PickChannel(param.account, channel));
                                return null;
                            }).join()));
        }), 0L, 1L, TimeUnit.HOURS);
    }

    /**
     * 升级卡片
     *
     * @param balance 当前余额
     * @param cards   卡片列表
     */
    @SuppressWarnings("unchecked")
    private void upgradeCard(BabyDogePawsAccount account, BigDecimal balance, List<Map<String, Object>> cards) {
        if (!IGNORED_USER.contains(account.chromeDataDirName) && balance.compareTo(BigDecimal.valueOf(10000000L)) <= 0) {
            cards.stream().flatMap((o) -> ((List<Map<String, Object>>) o.get("cards")).stream())
                    .filter((o) -> (Boolean) o.get("is_available"))
                    .sorted(Comparator.<Map<String, Object>, BigDecimal>comparing((card) -> (new BigDecimal(card.get("upgrade_cost").toString())).divide(new BigDecimal(card.get("farming_upgrade").toString()), 0, RoundingMode.HALF_UP))
                            .thenComparing((card) -> new BigDecimal(card.get("upgrade_cost").toString())))
                    .map((card) -> new Card((Integer) card.get("id"), new BigDecimal(card.get("upgrade_cost").toString())))
                    .findFirst().ifPresent((card) -> {
                        if (balance.compareTo(card.upgradeCost()) >= 0) {
                            var map = babyDogePawsApi.upgradeCard(new UpgradeCard(account, balance, card));
                            var latestBalance = new BigDecimal(map.getOrDefault("balance", BigDecimal.ZERO).toString());
                            var latestCards = (List<Map<String, Object>>) map.getOrDefault("cards", new ArrayList<>());
                            upgradeCard(account, latestBalance, latestCards);
                        }
                    });
        }

    }

    private static Optional<URI> inviteLink(String link) {
        try {
            URI uri = new URI(link);
            String scheme = uri.getScheme();
            if (scheme != null && (scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null) {
                return Optional.of(uri);
            } else {
                LOGGER.error("[无效的邀请链接]:{}", link);
                return Optional.empty();
            }
        } catch (Throwable t) {
            LOGGER.error("[无效的邀请链接]:{}", link, t);
            return Optional.empty();
        }
    }
}
