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
public class Account {

    public final BabyDogePawsUser user;
    public final String name;
    public final String chromeDataDir;
    public final String chromeDataDirName;
    public volatile Map<String, Object> data;

    public Account(BabyDogePawsUser user, String chromeRootDataDir) {
        this.user = user;
        this.name = user.phoneNumber;
        this.chromeDataDirName = user.areaCode + "-" + user.phoneNumber;
        this.chromeDataDir = chromeRootDataDir + chromeDataDirName;
    }


    public String xApiKey() {
        return getUserProperty("access_token", Object::toString, "");
    }

    public <T> T getUserProperty(String property, Function<Object, T> converter, T defaultValue) {
        return Optional.ofNullable(this.data)
                .map(o -> o.get(property))
                .map(o -> Functions.convert(o, converter, defaultValue))
                .orElse(defaultValue);
    }

    public int mineInterval() {
        int maxEnergy = getUserProperty("max_energy", o -> Integer.parseInt(o.toString()), 4);
        return Math.ceilDiv(maxEnergy, 4);
    }

    public int mineCount() {
        int maxEnergy = getUserProperty("max_energy", o -> Integer.parseInt(o.toString()), 4);
        int earnPerTap = getUserProperty("earn_per_tap", o -> Integer.parseInt(o.toString()), 4);
        return Math.ceilDiv(maxEnergy, earnPerTap);
    }

}
