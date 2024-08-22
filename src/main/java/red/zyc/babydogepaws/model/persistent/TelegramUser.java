package red.zyc.babydogepaws.model.persistent;

import java.time.LocalDateTime;

/**
 * @author allurx
 */
public record TelegramUser(

        Integer id,
        String country,
        String areaCode,
        String phoneNumber,
        Integer source,
        Integer banned,
        Integer passwordReset,
        Integer emailReset,
        LocalDateTime createdTime,
        LocalDateTime modifiedTime
) {
}
