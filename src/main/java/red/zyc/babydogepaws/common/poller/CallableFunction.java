package red.zyc.babydogepaws.common.poller;

import java.util.function.Function;

/**
 * 有返回值的函数
 *
 * @param <A> 函数输入类型
 * @param <B> 函数输出类型
 * @author allurx
 * @see Function#apply
 */
@FunctionalInterface
public non-sealed interface CallableFunction<A, B> extends Function<A, B>, PollerFunction<A, B> {

    @Override
    default B execute(A input) {
        return apply(input);
    }
}
