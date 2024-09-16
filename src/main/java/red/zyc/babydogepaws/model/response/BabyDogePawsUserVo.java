package red.zyc.babydogepaws.model.response;

import io.swagger.v3.oas.annotations.media.Schema;

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
    public String authParam;

}
