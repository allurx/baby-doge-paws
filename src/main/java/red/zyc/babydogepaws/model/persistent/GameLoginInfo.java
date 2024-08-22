package red.zyc.babydogepaws.model.persistent;

import java.time.LocalDateTime;

/**
 * 游戏登录信息
 *
 * @author allurx
 */
public record GameLoginInfo(

        Integer id,
        Integer userId,
        String authParam,
        Integer duration,
        LocalDateTime createdTime,
        LocalDateTime modifiedTime
) {

    public GameLoginInfo(Integer userId,
                         String authParam,
                         Integer duration) {
        this(null, userId, authParam, duration, null, null);
    }
}
