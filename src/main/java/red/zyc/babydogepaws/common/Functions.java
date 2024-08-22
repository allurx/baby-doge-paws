package red.zyc.babydogepaws.common;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author allurx
 */
public class Functions {

    private Functions() {
    }

    public static <A, B> B convert(A value, Function<A, B> converter, B defaultValue) {
        return Optional.ofNullable(value).map(converter).orElse(defaultValue);
    }
}
