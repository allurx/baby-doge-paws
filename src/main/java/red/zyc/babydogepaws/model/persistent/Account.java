package red.zyc.babydogepaws.model.persistent;

import java.time.LocalDateTime;

/**
 * @author allurx
 */
public class Account {

    public Integer id;
    public Integer userId;
    public String inviteLink;
    public LocalDateTime createdTime;
    public LocalDateTime modifiedTime;

    public Account(Integer userId, String inviteLink) {
        this.userId = userId;
        this.inviteLink = inviteLink;
    }
}
