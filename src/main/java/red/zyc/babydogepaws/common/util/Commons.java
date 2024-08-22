package red.zyc.babydogepaws.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * @author allurx
 */
public final class Commons {

    private static final Logger LOGGER = LoggerFactory.getLogger(Commons.class);

    private Commons() {
    }

    /**
     * 将{@link Throwable}转换为字符串
     *
     * @param throwable {@link Throwable}
     * @return {@link Throwable}打印后的字符串
     */
    public static String convertThrowableToString(Throwable throwable) {
        return Optional.of(throwable).map((t) -> {
            StringWriter stringWriter = new StringWriter();
            t.printStackTrace(new PrintWriter(stringWriter, true));
            return stringWriter.getBuffer().toString();
        }).get();
    }

    /**
     * 包装{@link Runnable}，使其在执行run方法时不会抛出异常
     *
     * @param r {@link Runnable}
     * @return 执行run方法时不会抛出异常的 {@link Runnable}
     */
    public static Runnable safeRunnable(Runnable r) {
        return () -> {
            try {
                r.run();
            } catch (Throwable t) {
                LOGGER.error(t.getMessage(), t);
            }
        };
    }
}
