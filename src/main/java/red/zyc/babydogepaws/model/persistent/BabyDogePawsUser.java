package red.zyc.babydogepaws.model.persistent;

import red.zyc.babydogepaws.common.util.ApplicationContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * @author allurx
 */
public class BabyDogePawsUser {

    public static final int MINE_INTERVAL = 3000 / 4;
    public static final int MINE_COUNT = 300;

    public Integer id;
    public String country;
    public String areaCode;
    public String phoneNumber;
    public Integer source;
    public Integer banned;
    public Integer passwordReset;
    public Integer emailReset;
    public String inviteLink;
    public String friendNum;

    public volatile String xApiKey;
    public volatile String authParam;
    public volatile Map<String, Object> data;
    public volatile List<ScheduledFuture<?>> tasks = new ArrayList<>();
    public volatile boolean tasksCanceled = false;

    public String chromeDataDir() {
        return ApplicationContextHolder.getProperty("baby-doge-paws.chrome.root-data-dir", String.class) + areaCode + "-" + phoneNumber;
    }

    /**
     * 取消用户的所有任务
     */
    public void cancelAllTask() {
        tasks.forEach(scheduledFuture -> scheduledFuture.cancel(false));
        tasksCanceled = true;
    }
}
