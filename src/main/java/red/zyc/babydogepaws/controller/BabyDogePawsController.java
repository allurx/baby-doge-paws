package red.zyc.babydogepaws.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import red.zyc.babydogepaws.game.BabyDogePawsApi;
import red.zyc.babydogepaws.game.BabyDogePawsTask;
import red.zyc.babydogepaws.dao.UserMapper;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
import red.zyc.babydogepaws.model.response.BabyDogePawsUserVo;
import red.zyc.babydogepaws.model.response.base.Response;
import red.zyc.babydogepaws.model.response.base.ResponseMessage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static red.zyc.babydogepaws.common.constant.Constants.VOID;
import static red.zyc.babydogepaws.model.response.base.Response.ok;
import static red.zyc.kit.json.JsonOperator.JACKSON_OPERATOR;

/**
 * @author allurx
 */
@Tag(name = "BabyDogePaws", description = "BabyDogePaws Api")
@RestController
public class BabyDogePawsController {

    private final UserMapper userMapper;
    private final BabyDogePawsApi babyDogePawsApi;
    private final BabyDogePawsTask babyDogePawsTask;

    public BabyDogePawsController(UserMapper userMapper, BabyDogePawsApi babyDogePawsApi, BabyDogePawsTask babyDogePawsTask) {
        this.userMapper = userMapper;
        this.babyDogePawsApi = babyDogePawsApi;
        this.babyDogePawsTask = babyDogePawsTask;
    }


    @Operation(summary = "获取用户信息")
    @GetMapping("/getUser")
    public Response<BabyDogePawsUserVo> getUser(//@Parameter(ref = PARAMETER_COMPONENT_USER_PHONE_NUMBER)
                                                @RequestParam String phoneNumber) {
        return ok(Optional.ofNullable(userMapper.getBabyDogeUser(phoneNumber))
                .map(user -> JACKSON_OPERATOR.copyProperties(userMapper.getBabyDogeUser(phoneNumber), BabyDogePawsUserVo.class))
                .orElse(null));
    }

    @Operation(summary = "获取用户好友信息")
    @GetMapping("/listFriends")
    public Response<Map<String, Object>> listFriends(//@Parameter(ref = PARAMETER_COMPONENT_USER_PHONE_NUMBER)
                                                     String phoneNumber) {
        return ok(Optional.ofNullable(userMapper.getBabyDogeUser(phoneNumber))
                .map(BabyDogePawsGameRequestParam::new)
                .map(babyDogePawsApi::listFriends)
                .orElse(new HashMap<>()));
    }

    @Operation(summary = "启动用户所有定时任务")
    @PostMapping("/bootstrap")
    public Response<Void> bootstrap(//@Parameter(ref = PARAMETER_COMPONENT_USER_PHONE_NUMBER)
                                    String phoneNumber) {
        return Optional.ofNullable(userMapper.getBabyDogeUser(phoneNumber))
                .map(user -> {
                    babyDogePawsTask.schedule(new BabyDogePawsGameRequestParam(user));
                    return ok(VOID);
                })
                .orElse(ok(ResponseMessage.MISSING_USER));
    }

    @Operation(summary = "获取用户所有卡片的升级信息")
    @GetMapping("/listCardUpgradeInfo")
    public Response<List<Map<String, String>>> listCardUpgradeInfo(//@Parameter(ref = PARAMETER_COMPONENT_USER_PHONE_NUMBER)
                                                                   String phoneNumber) {
        return ok(Optional.ofNullable(userMapper.getBabyDogeUser(phoneNumber))
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
                .orElse(new ArrayList<>()));
    }
}
