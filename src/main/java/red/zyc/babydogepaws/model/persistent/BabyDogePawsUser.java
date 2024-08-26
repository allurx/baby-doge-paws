package red.zyc.babydogepaws.model.persistent;

import red.zyc.babydogepaws.common.Functions;
import red.zyc.babydogepaws.common.util.ApplicationContextHolder;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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
    public String xApiKey;
    public String friendNum;
    public volatile String authParam;
    public volatile Map<String, Object> data;

    public String xApiKey() {
        return getUserProperty("access_token", Object::toString, "");
    }

    public <T> T getUserProperty(String property, Function<Object, T> converter, T defaultValue) {
        return Optional.ofNullable(data)
                .map(o -> o.get(property))
                .map(o -> Functions.convert(o, converter, defaultValue))
                .orElse(defaultValue);
    }

    public String chromeDataDir() {
        return ApplicationContextHolder.getProperty("chrome.root-data-dir", String.class) + areaCode + "-" + phoneNumber;
    }
}
