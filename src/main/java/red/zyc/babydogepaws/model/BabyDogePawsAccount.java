package red.zyc.babydogepaws.model;

import red.zyc.babydogepaws.common.Functions;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * telegram用户
 *
 * @author allurx
 */
public class BabyDogePawsAccount {

    public static final int MINE_INTERVAL = 3000 / 4;
    public static final int MINE_COUNT = 300;
    public final BabyDogePawsUser user;
    public final String name;
    public final String chromeDataDir;
    public final String chromeDataDirName;
    public Map<String, Object> data;
    public boolean dataValid = false;

    public BabyDogePawsAccount(BabyDogePawsUser user, String chromeRootDataDir) {
        this.user = user;
        this.name = user.phoneNumber;
        this.chromeDataDirName = user.areaCode + "-" + user.phoneNumber;
        this.chromeDataDir = chromeRootDataDir + chromeDataDirName;
    }

    public String xApiKey() {
        return getUserProperty("access_token", Object::toString, "");
    }

    public <T> T getUserProperty(String property, Function<Object, T> converter, T defaultValue) {
        return Optional.ofNullable(data)
                .map(o -> o.get(property))
                .map(o -> Functions.convert(o, converter, defaultValue))
                .orElse(defaultValue);
    }

}
