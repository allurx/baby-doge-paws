package red.zyc.babydogepaws.common.constant;

import red.zyc.toolkit.core.reflect.TypeToken;

import java.util.List;
import java.util.Map;

/**
 * @author allurx
 */
public final class Constants {

    private Constants() {
    }

    public static final TypeToken<Map<String, Object>> OBJECT_DATA_TYPE = new TypeToken<Map<String, Object>>() {
    };
    public static final TypeToken<List<Map<String, Object>>> LIST_OBJECT_DATA_TYPE = new TypeToken<List<Map<String, Object>>>() {
    };

    public static final String JSON_CONTENT_TYPE = "application/json";

    public static final Void VOID = null;


}
