package red.zyc.babydogepaws;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.zyc.babydogepaws.common.constant.Constants;
import red.zyc.babydogepaws.game.BabyDogePawsApi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static red.zyc.kit.json.JsonOperator.JACKSON_OPERATOR;


/**
 * @author allurx
 */
public class TODO {

    private static final Logger LOGGER = LoggerFactory.getLogger(TODO.class);
    public final TODO TODO = this;


    /**
     * <ol>
     *     <li>
     *         swagger-ui接口请求参数无法复用Components
     *     </li>
     *     <li>
     *         过滤器无法处理controller请求参数不正确时抛出的异常，spring解析参数发生在过滤器执行前？？？
     *     </li>
     *     <li>
     *         是否可以复用一个webdriver
     *     </li>
     *     <li>
     *         {@link BabyDogePawsApi}方法入参优化，应该是最底层的外部接口实际入参
     *     </li>
     *     <li>
     *         优化挖矿间隔和次数计算逻辑
     *     </li>
     *     <li>
     *         {@link ExpectedConditions#elementToBeClickable(By)}方法无法保证元素一定能够点击，需要尝试多种点击方式
     *     </li>
     * </ol>
     */
    public TODO() {
    }

    public static void main(String[] args) throws Exception {

        CompletableFuture.supplyAsync(() -> 1)
                .thenApply(i -> ++i)
                .thenApply(i -> {
                    System.out.println(i);
                    return i;
                })
                .thenRun(() -> System.out.println(1))
                .thenRun(() -> System.out.println(2));
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
