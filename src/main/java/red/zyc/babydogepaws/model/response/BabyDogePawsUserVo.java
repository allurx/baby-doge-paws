package red.zyc.babydogepaws.model.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * @author allurx
 */
@Schema(description = "BabyDogePaws用户信息")
public class BabyDogePawsUserVo {

    @Schema(description = "邀请链接")
    public String inviteLink;

    @Schema(description = "x-api-key")
    public String xApiKey;

    @Schema(description = "好友数量")
    public String friendNum;

    @Schema(description = "授权参数")
    public volatile String authParam;

    @Schema(description = "游戏数据")
    public volatile Map<String, Object> data;

    @Schema(description = "定时任务")
    public volatile List<ScheduledFuture<?>> tasks = new ArrayList<>();

    @Schema(description = "定时任务是否被取消了")
    public volatile boolean tasksCanceled = false;


}
