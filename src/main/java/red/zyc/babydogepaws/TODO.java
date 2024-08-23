package red.zyc.babydogepaws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.zyc.babydogepaws.common.NamedThreadFactory;
import red.zyc.babydogepaws.common.Poller;
import red.zyc.babydogepaws.common.constant.Constants;
import red.zyc.babydogepaws.core.BabyDogePaws;
import red.zyc.babydogepaws.model.BabyDogePawsAccount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Supplier;

import static red.zyc.toolkit.json.Json.JACKSON_OPERATOR;

/**
 * @author allurx
 */
public class TODO {

    private static final Logger LOGGER = LoggerFactory.getLogger(TODO.class);
    private static final ScheduledExecutorService EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(Integer.MAX_VALUE, new NamedThreadFactory("TODO", false));
    public final TODO TODO = this;


    /**
     * <ol>
     *     <li>{@link BabyDogePaws#playBabyDogePaws(BabyDogePawsAccount, int)}优化</li>
     *     <li>
     *         是否可以复用一个webdriver
     *     </li>
     *     <li>
     *         {@link Poller#pollWhenMiss(Supplier)}优化
     *     </li>
     *     <li>
     *         优化挖矿间隔和次数计算逻辑
     *     </li>
     * </ol>
     */
    public TODO() {
    }

    public static void main(String[] args) throws Exception {
        cardRelated();
    }

    @SuppressWarnings("unchecked")
    static void cardRelated() throws Exception {
        JACKSON_OPERATOR.fromJsonString(Files.readString(Path.of("C:\\Users\\zyc\\Desktop\\sublime\\baby_doge_paws\\cards.json")), Constants.LIST_OBJECT_DATA_TYPE)
                .stream()
                .flatMap((map) -> ((List<Map<String, Object>>) map.get("cards")).stream())
                .filter((o) -> (Boolean) o.get("is_available"))
                .sorted(Comparator.<Map<String, Object>, BigDecimal>comparing((card) -> (new BigDecimal(card.get("upgrade_cost").toString())).divide(new BigDecimal(card.get("farming_upgrade").toString()), 0, RoundingMode.HALF_UP))
                        .thenComparing((card) -> new BigDecimal(card.get("upgrade_cost").toString())))
                .peek((card) -> {
                    String price = String.valueOf((new BigDecimal(card.get("upgrade_cost").toString())).divide(new BigDecimal(card.get("farming_upgrade").toString()), 0, RoundingMode.HALF_UP));
                    String name = String.valueOf(card.get("name"));
                    String upgradeCost = String.valueOf(card.get("upgrade_cost"));
                    String farmingUpgrade = String.valueOf(card.get("farming_upgrade"));
                    System.out.println(price + ":" + name + ":" + upgradeCost + ":" + farmingUpgrade);
                }).toList();
    }
}
