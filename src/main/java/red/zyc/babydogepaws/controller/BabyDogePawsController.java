package red.zyc.babydogepaws.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import red.zyc.babydogepaws.core.BabyDogePawsApi;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author allurx
 */
@RestController
@RequestMapping
public class BabyDogePawsController {

    private final List<BabyDogePawsUser> users;
    private final BabyDogePawsApi babyDogePawsApi;

    public BabyDogePawsController(@Qualifier("users") List<BabyDogePawsUser> users,
                                  BabyDogePawsApi babyDogePawsApi) {
        this.users = users;
        this.babyDogePawsApi = babyDogePawsApi;
    }

    /**
     * 获取用户信息
     *
     * @param phoneNumber 用户手机号码
     * @return 用户信息
     */
    @GetMapping("/getUser")
    public BabyDogePawsUser getUser(String phoneNumber) {
        return users.stream()
                .filter(user -> Objects.equals(user.phoneNumber, phoneNumber))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取用户好友信息
     *
     * @param phoneNumber 用户手机号码
     * @return 用户好友信息
     */
    @GetMapping("/listFriends")
    public Map<String, Object> listFriends(String phoneNumber) {
        return users.stream()
                .filter(user -> Objects.equals(user.phoneNumber, phoneNumber))
                .findFirst()
                .map(user -> babyDogePawsApi.listFriends(new BabyDogePawsGameRequestParam(user)))
                .orElse(new HashMap<>());
    }

    /**
     * 获取用户所有卡的升级信息
     *
     * @param phoneNumber 用户手机号码
     * @return 卡的升级信息
     */
    @GetMapping("/listCardUpgradeInfo")
    public List<Map<String, String>> listCardUpgradeInfo(String phoneNumber) {
        return users.stream()
                .filter(user -> Objects.equals(user.phoneNumber, phoneNumber))
                .findFirst()
                .map(BabyDogePawsGameRequestParam::new)
                .map(babyDogePawsApi::listCards)
                .map(cards -> cards.stream()
                        .flatMap(o -> ((List<Map<String, Object>>) o.get("cards")).stream().peek(card -> card.put("categoryName", o.get("name"))))
                        .filter(o -> (Boolean) o.get("is_available"))
                        .sorted(Comparator.<Map<String, Object>, BigDecimal>comparing((card) -> (new BigDecimal(card.get("upgrade_cost").toString())).divide(new BigDecimal(card.get("farming_upgrade").toString()), 0, RoundingMode.HALF_UP))
                                .thenComparing((card) -> new BigDecimal(card.get("upgrade_cost").toString())))
                        .map(card -> {
                            String price = String.valueOf((new BigDecimal(card.get("upgrade_cost").toString())).divide(new BigDecimal(card.get("farming_upgrade").toString()), 2, RoundingMode.HALF_UP));
                            String name = String.valueOf(card.get("name"));
                            String categoryName = String.valueOf(card.get("categoryName"));
                            String upgradeCost = String.valueOf(card.get("upgrade_cost"));
                            String farmingUpgrade = String.valueOf(card.get("farming_upgrade"));
                            String curTotalFarming = String.valueOf(card.get("cur_total_farming"));
                            return Map.of(
                                    "categoryName", categoryName,
                                    "price", price,
                                    "name", name,
                                    "upgradeCost", upgradeCost,
                                    "farmingUpgrade", farmingUpgrade,
                                    "curTotalFarming", curTotalFarming
                            );
                        })
                        .toList()
                )
                .orElse(new ArrayList<>());
    }
}
