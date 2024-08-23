package red.zyc.babydogepaws.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import red.zyc.babydogepaws.core.BabyDogePawsApi;
import red.zyc.babydogepaws.dao.UserMapper;
import red.zyc.babydogepaws.model.BabyDogePawsAccount;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author allurx
 */
@RestController
@RequestMapping
public class BabyDogePawsController {

    @Value("${chrome.root-data-dir}")
    private String chromeRootDataDir;

    private final BabyDogePawsApi babyDogePawsApi;
    private final UserMapper userMapper;

    public BabyDogePawsController(BabyDogePawsApi babyDogePawsApi, UserMapper userMapper) {
        this.babyDogePawsApi = babyDogePawsApi;
        this.userMapper = userMapper;
    }

    @GetMapping("/listCardUpgradePrice")
    public List<Map<String, String>> listCardUpgradePrice(Integer userId) {
        return Optional.ofNullable(userMapper.getBabyDogeUser(userId))
                .map(babyDogeUser -> new BabyDogePawsGameRequestParam(new BabyDogePawsAccount(babyDogeUser, chromeRootDataDir)))
                .map(babyDogePawsApi::listCards)
                .map(cards -> cards.stream().flatMap(o -> ((List<Map<String, Object>>) o.get("cards")).stream().map(card -> {
                            card.put("categoryName", o.get("name"));
                            return card;
                        }))
                        .filter(o -> (Boolean) o.get("is_available"))
                        .sorted(Comparator.<Map<String, Object>, BigDecimal>comparing((card) -> (new BigDecimal(card.get("upgrade_cost").toString())).divide(new BigDecimal(card.get("farming_upgrade").toString()), 0, RoundingMode.HALF_UP))
                                .thenComparing((card) -> new BigDecimal(card.get("upgrade_cost").toString())))
                        .map(card -> {
                            String price = String.valueOf((new BigDecimal(card.get("upgrade_cost").toString())).divide(new BigDecimal(card.get("farming_upgrade").toString()), 2, RoundingMode.HALF_UP));
                            String name = String.valueOf(card.get("name"));
                            String categoryName = String.valueOf(card.get("categoryName"));
                            String upgradeCost = String.valueOf(card.get("upgrade_cost"));
                            String farmingUpgrade = String.valueOf(card.get("farming_upgrade"));
                            return Map.of("categoryName", categoryName, "price", price, "name", name, "upgradeCost", upgradeCost, "farmingUpgrade", farmingUpgrade);
                        }).toList())
                .orElse(new ArrayList<>());
    }
}
