package red.zyc.babydogepaws.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * @author allurx
 */
public final class Commons {

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
     * 判断字符串是否为空
     *
     * @param str 字符串
     * @return 字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * {@link Object#getClass()}返回是通配符类型的{@link Class}，
     * 可以通过这个方法获取指定类型对象的{@link Class}。
     *
     * @param value 对象值
     * @param <T>   对象类型
     * @return 指定类型对象的 {@link Class}
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClass(T value) {
        return (Class<T>) value.getClass();
    }
}
