package red.zyc.babydogepaws.common.poller;

/**
 * 轮询器控制和标记函数
 *
 * @param <A> 函数输入类型
 * @param <B> 函数输出类型
 * @author allurx
 * @see RunnableFunction
 * @see CallableFunction
 */
public sealed interface PollerFunction<A, B> permits RunnableFunction, CallableFunction {

    /**
     * 子类必须实现这个方法用来执行实际的函数
     *
     * @param input 函数输入参数类型
     * @return 函数输出类型
     */
    default B execute(A input) {
        throw new IllegalStateException("%s必须实现该方法执行实际的函数".formatted(getClass().getName()));
    }

}
