package red.zyc.babydogepaws.model.persistent;

import java.time.LocalDateTime;

/**
 * @author allurx
 */
public class TelegramUser {

    public Integer id;
    public String country;
    public String areaCode;
    public String phoneNumber;
    public Integer source;
    public Integer banned;
    public Integer passwordReset;
    public Integer emailReset;
    public LocalDateTime createdTime;
    public LocalDateTime modifiedTime;
}
