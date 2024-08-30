package red.zyc.babydogepaws.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.babydogepaws.model.persistent.Card;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;

import static red.zyc.toolkit.json.Json.JACKSON_OPERATOR;

/**
 * @author allurx
 */
public class UpgradeCard extends BabyDogePawsGameRequestParam {

    public final BigDecimal balance;
    public final Card card;
    public final UpgradeInfo upgradeInfo;
    public final boolean available;

    public UpgradeCard(BabyDogePawsUser user,
                       BigDecimal balance,
                       Card card,
                       UpgradeInfo upgradeInfo,
                       boolean available) {
        super(user);
        this.balance = balance;
        this.card = card;
        this.upgradeInfo = upgradeInfo;
        this.upgradeInfo.buildJsonFunctionParam();
        this.available = available;
    }

    public static class UpgradeInfo {

        @JsonProperty("cur_level")
        public String level;
        @JsonProperty("upgrade_cost")
        public BigDecimal cost;
        @JsonProperty("farming_upgrade")
        public BigDecimal profit;

        public String insertJson;
        public String updateJson;
        public String levelPath;

        // 升级的信息map，需要在运行时转换成json字符串
        private final Function<UpgradeInfo, Map<String, BigDecimal>> func = o -> Map.of("cost", cost, "profit", profit);

        // 构造mysql json_set函数的入参
        public void buildJsonFunctionParam() {

            // json的key如果是数字类型的字符串，必须手动加上双引号
            levelPath = String.format("$.\"%s\"", level);
            var map = func.apply(this);
            insertJson = JACKSON_OPERATOR.toJsonString(Map.of(level, map));
            updateJson = JACKSON_OPERATOR.toJsonString(map);
        }

    }

    public static class JsonObjectToJsonStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext dc) throws IOException {
            var objectMapper = (ObjectMapper) p.getCodec();
            Object jsonNode = objectMapper.readTree(p);
            return objectMapper.writeValueAsString(jsonNode);
        }
    }
}
