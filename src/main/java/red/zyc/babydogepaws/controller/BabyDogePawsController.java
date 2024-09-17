package red.zyc.babydogepaws.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import red.zyc.babydogepaws.dao.UserMapper;
import red.zyc.babydogepaws.game.BabyDogePawsApi;
import red.zyc.babydogepaws.game.BabyDogePawsTask;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
import red.zyc.babydogepaws.model.request.FarmAll;
import red.zyc.babydogepaws.model.request.FarmAllExclude;
import red.zyc.babydogepaws.model.request.ResolveChannel;
import red.zyc.babydogepaws.model.response.BabyDogePawsUserVo;
import red.zyc.babydogepaws.model.response.Channel;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static red.zyc.babydogepaws.common.constant.Constants.VOID;
import static red.zyc.babydogepaws.model.response.base.Response.ok;
import static red.zyc.babydogepaws.model.response.base.ResponseMessage.*;
import static red.zyc.kit.json.JsonOperator.JACKSON_OPERATOR;

/**
 * @author allurx
 */
@Tag(name = "BabyDogePaws", description = "BabyDogePaws Api")
@RestController
public class BabyDogePawsController {

    private static final ExecutorService NEW_VIRTUAL_THREAD_PER_TASK_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private final UserMapper userMapper;
    private final BabyDogePawsApi babyDogePawsApi;
    private final BabyDogePawsTask babyDogePawsTask;

    public BabyDogePawsController(UserMapper userMapper, BabyDogePawsApi babyDogePawsApi, BabyDogePawsTask babyDogePawsTask) {
        this.userMapper = userMapper;
        this.babyDogePawsApi = babyDogePawsApi;
        this.babyDogePawsTask = babyDogePawsTask;
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

    @Operation(summary = "给单个用户刷paws")
    @PostMapping("/farm")
    public Response<Void> farm(@RequestParam String phoneNumber, @RequestParam long amount) {
        if (amount <= 0) {
            return ok(ILLEGAL_FARM_AMOUNT);
        }
        return Optional.ofNullable(userMapper.getBabyDogeUser(phoneNumber))
                .map(BabyDogePawsGameRequestParam::new)
                .flatMap(param -> ((List<Map<String, Object>>) babyDogePawsApi.listChannels(param).getOrDefault("channels", new ArrayList<>()))
                        .stream()
                        .map(channel -> new Channel(
                                Long.parseLong(channel.get("id").toString()),
                                Long.parseLong(channel.get("reward").toString()),
                                (boolean) channel.get("is_resolved"),
                                (boolean) channel.get("is_reward_taken"),
                                (boolean) channel.get("is_premium")))
                        .filter(channel -> channel.isResolved() && channel.isRewardTaken())
                        .max(Comparator.comparing(Channel::reward))
                        .map(channel -> new ResolveChannel(param.user, channel)))
                .map(resolveChannel -> {
                    int times = (int) Math.ceilDiv(amount, resolveChannel.channel.reward());
                    IntStream.range(0, times).parallel().forEach(value -> babyDogePawsApi.pickChannel(resolveChannel));
                    return ok(VOID);
                }).orElse(ok(NO_COMPLETED_TASKS_WITH_REWARDS));

    }

    @Operation(summary = "给指定的用户集合刷paws")
    @PostMapping("/farmAll")
    public Response<Void> farmAll(@RequestBody FarmAll farmAll) {
        if (farmAll.amount <= 0) {
            return ok(ILLEGAL_FARM_AMOUNT);
        }
        farmAll.phoneNumbers.forEach(phoneNumber -> NEW_VIRTUAL_THREAD_PER_TASK_EXECUTOR.execute(() -> farm(phoneNumber, farmAll.amount)));
        return ok();
    }

    @Operation(summary = "给指定的用户集合之外的所有用户刷paws")
    @PostMapping("/farmAllExclude")
    public Response<Void> farmAllExclude(@RequestBody FarmAllExclude farmAllExclude) {
        if (farmAllExclude.amount <= 0) {
            return ok(ILLEGAL_FARM_AMOUNT);
        }
        userMapper.listBabyDogeUsers()
                .stream()
                .filter(user -> !farmAllExclude.excludedPhoneNumbers.contains(user.phoneNumber))
                .forEach(user -> NEW_VIRTUAL_THREAD_PER_TASK_EXECUTOR.execute(() -> farm(user.phoneNumber, farmAllExclude.amount)));
        return ok();
    }

    @Operation(summary = "修改挖矿数量")
    @PostMapping("/updateMineCount")
    public Response<Void> updateMineCount(@RequestParam int mineCountMin, @RequestParam int mineCountMax) {
        if (mineCountMin >= mineCountMax) {
            return ok(ILLEGAL_MINE_COUNT);
        } else {
            BabyDogePawsTask.mineCountMin = mineCountMin;
            BabyDogePawsTask.mineCountMax = mineCountMax;
            return ok();
        }
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/getUser")
    public Response<BabyDogePawsUserVo> getUser(//@Parameter(ref = PARAMETER_COMPONENT_USER_PHONE_NUMBER)
                                                @RequestParam String phoneNumber) {
        return ok(Optional.ofNullable(userMapper.getBabyDogeUser(phoneNumber))
                .map(user -> JACKSON_OPERATOR.<BabyDogePawsUserVo>copyProperties(user, BabyDogePawsUserVo.class))
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
