package red.zyc.babydogepaws.model.persistent;

import java.time.LocalDateTime;

/**
 * @author allurx
 */
public class User {

    public Integer id;
    public Integer userId;
    public String inviteLink;
    public String xApiKey;
    public Integer friendNum;
    public LocalDateTime createdTime;
    public LocalDateTime modifiedTime;

}
