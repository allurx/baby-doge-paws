package red.zyc.babydogepaws.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户登录信息
 *
 * @author allurx
 */
public class LoginInfo {

    public int failNum = 0;
    public List<LoginHistory> histories = new ArrayList<>();

    public record LoginHistory(LocalDateTime loginTime, long loginDuration) {
    }
}
