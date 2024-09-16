package red.zyc.babydogepaws.model.response.base;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 响应信息枚举
 * <ul>
 *     <li>S开头的响应码代表本次请求是成功的</li>
 *     <li>F开头的响应码代表本次请求是失败的</li>
 * </ul>
 *
 * @author allurx
 */
@Schema(description = "响应信息")
public enum ResponseMessage {

    /**
     * system
     */
    SUCCESS("成功", "S0000"),
    FAIL("失败", "F0000"),
    INTERNAL_SERVER_ERROR("系统繁忙，请稍后再试", "F0001"),


    UN_AUTHORIZED("未授权", "F0002"),
    MISSING_USER("用户不存在", "F0003"),

    ILLEGAL_MINE_COUNT("mineCountMin必须小于mineCountMax", "F0004"),


    ;

    @Schema(description = "响应码")
    public final String code;

    @Schema(description = "响应信息")
    public final String message;

    ResponseMessage(String message, String code) {
        this.message = message;
        this.code = code;
    }
}
