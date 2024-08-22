package red.zyc.babydogepaws.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.zyc.babydogepaws.common.constant.Constants;
import red.zyc.babydogepaws.core.BabyDogePawsApi;
import red.zyc.babydogepaws.model.User;

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
     *     <li>初始化时，不是所有用户都需要登录一下tg获取授权参数，
     *     如果游戏登录信息表中，登录记录的修改时间没有超过1天的话（游戏那边好像是1天过期），
     *     直接用上次的授权参数进行授权就行
     *     </li>
     *     <li>
     *         同一个用户执行{@link BabyDogePawsApi#authorize(User)}方法应该与其它方法互斥，
     *         因为其它方法都是依赖与authorize方法返回的access_token
     *     </li>
     *     <li>
     *         添加一个game_user_config表，标记读取游戏用户时哪些用户不需要进行自动化，
     *         一些重要的账户暂时还是手动操作比较好
     *     </li>
     *     <li>
     *         {@link Poller#pollWhenMiss(Supplier)}优化
     *     </li>
     *     <li></li>
     *     <li></li>
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
