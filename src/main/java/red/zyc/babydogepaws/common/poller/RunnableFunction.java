package red.zyc.babydogepaws.common.poller;

/**
 * 没有返回值的函数
 *
 * @param <T> 函数输入类型
 * @author allurx
 */
@FunctionalInterface
public non-sealed interface RunnableFunction<T> extends PollerFunction<T, Void> {

    /**
     * 没有返回值的函数方法
     *
     * @param input 函数输入
     */
    void run(T input);

    @Override
    default Void execute(T a) {
        run(a);
        return null;
    }

}
