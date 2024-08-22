package red.zyc.babydogepaws.selenium;

import java.util.HashMap;
import java.util.Map;

/**
 * @author allurx
 */
public final class BabyDogePawsContext {

    private static final ThreadLocal<Map<String, Object>> CONTEXT_HOLDER = ThreadLocal.withInitial(HashMap::new);

    private BabyDogePawsContext() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) CONTEXT_HOLDER.get().get(key);
    }

    public static void set(String key, Object value) {
        CONTEXT_HOLDER.get().put(key, value);
    }

    public static void remove() {
        CONTEXT_HOLDER.remove();
    }
}
