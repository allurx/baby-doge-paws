package red.zyc.babydogepaws.common.poller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.zyc.toolkit.core.function.ThrowableSupplier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;

/**
 * 在一段时间内以固定的时间间隔执行{@link PollerFunction}，直到超时或者函数的输出满足特定的条件，下面展示了两种常见的使用场景
 *
 * <pre>
 * <h6 color="yellow">在10秒内，每隔500毫秒打印当前时间，直到超时然后抛出RuntimeException</h6>
 * {@code
 *     Poller.<Void, Void>builder()
 *              .timing(Duration.ofSeconds(10), Duration.ofMillis(500))
 *              .<RunnableFunction<Void>>execute(null, o -> System.out.println(LocalDateTime.now()))
 *              .predicate(o -> false)
 *              .onTimeout(throwingRunnable(() -> new RuntimeException("RuntimeException")))
 *              .build()
 *              .poll();
 * }
 * </pre>
 *
 * <pre>
 * <h6 color="yellow">在10秒内，每隔500毫秒打印num++，直到num == 12则退出轮询</h6>
 * {@code
 *         AtomicInteger num = new AtomicInteger(1);
 *         Poller.<AtomicInteger, Integer>builder()
 *                 .timing(Duration.ofSeconds(10), Duration.ofMillis(500))
 *                 .<CallableFunction<AtomicInteger, Integer>>execute(num, i -> {
 *                     System.out.println(num.get());
 *                     return num.getAndIncrement();
 *                 })
 *                 .predicate(o -> o == 12)
 *                 .onTimeout(throwingRunnable(() -> new RuntimeException("RuntimeException")))
 *                 .build()
 *                 .poll();
 * }
 * </pre>
 *
 * @param <A> 轮询器执行函数的输入类型
 * @param <B> 轮询器执行函数的返回类型
 * @author allurx
 */
public class Poller<A, B> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Poller.class);

    private Poller() {
    }

    /**
     * {@link Clock}
     */
    private Clock clock;

    /**
     * 轮询的最大时长
     */
    private Duration duration;

    /**
     * 每次轮询的间隔
     */
    private Duration interval;

    /**
     * 轮询期间执行的函数
     */
    private PollerFunction<A, B> function;

    /**
     * {@link #function}的输入
     */
    private A input;

    /**
     * {@link #function}的输出满足该条件则退出轮询
     */
    private Predicate<B> predicate;

    /**
     * {@link Sleeper}
     */
    private Sleeper sleeper;

    /**
     * 超时退出轮询时执行的操作
     */
    private Runnable timeoutAction;

    /**
     * {@link #function}执行期间忽略的异常
     */
    private final List<Class<? extends Throwable>> ignoredExceptions = new ArrayList<>();

    /**
     * 在{@link Clock#instant()}+{@link #duration}时间段内，以固定的{@link #interval}执行{@link #function}，
     * 直到发生以下任何一种情况才会退出轮询
     * <ul>
     *     <li>{@link #function}执行结果满足{@link #predicate}
     *     <ul>
     *         <li>{@link PollResult#success} == true</li>
     *         <li>{@link PollResult#timeout} == false</li>
     *     </ul>
     *     </li>
     *     <li>（超时）总时长超过{@link Clock#instant()}+{@link #duration}
     *     <ul>
     *         <li>{@link PollResult#timeout} == true</li>
     *         <li>{@link PollResult#success} == false</li>
     *     </ul>
     *     </li>
     * </ul>
     *
     * @return {@link #function}执行结果
     */
    public Optional<B> poll() {
        return polling();
    }

    /**
     * 轮询结果
     */
    private class PollResult {

        /**
         * 轮询的次数
         */
        private int num = 0;

        /**
         * 函数的输出
         */
        private B output = null;

        /**
         * 轮询是否超时
         */
        private boolean timeout = false;

        /**
         * 函数是否执行成功
         */
        private boolean success = false;
    }

    /**
     * 轮询逻辑
     */
    private Optional<B> polling() {
        PollResult result = new PollResult();
        Instant endInstant = clock.instant().plus(duration);
        for (; ; ) {

            // 执行function
            execute(result);

            // 函数执行成功则退出轮询
            if (result.success) break;

            // 以下两种情况就没有必要sleep了
            // 1.function执行完成后剩余的时间不足interval
            // 2.function执行完成后的时刻已经超过endInstant
            result.timeout = clock.instant().plus(interval).isAfter(endInstant);
            if (result.timeout) {
                timeoutAction.run();
                break;
            }

            // sleep
            sleeper.sleep(interval);
        }
        return Optional.ofNullable(result.output);
    }

    /**
     * 如果{@link #function}执行期间发生异常，并且这个异常不在{@link #ignoredExceptions}中，则抛出它
     */
    private void execute(PollResult result) {
        try {
            result.num++;
            result.output = function.execute(input);
            result.success = predicate.test(result.output);
        } catch (Throwable t) {
            if (ignoredExceptions.stream().noneMatch(ignoredException -> ignoredException.isInstance(t))) throw t;
            LOGGER.warn("Poller is ignoring the exception: {}", t.getClass().getName());
        }
    }

    /**
     * 创建一个运行时抛出由throwableSupplier提供{@link RuntimeException}的{@link Runnable}
     *
     * @param throwableSupplier {@link RuntimeException}提供器
     * @return {@link Runnable}
     */
    public static Runnable throwingRunnable(ThrowableSupplier<? extends RuntimeException> throwableSupplier) {
        return () -> {
            throw throwableSupplier.get();
        };
    }

    // builder methods

    public static <A, B> Builder<A, B> builder() {
        return new Builder<>(new Poller<>());
    }

    /**
     * 构建{@link Poller}的builder
     *
     * @param <A> {@link #function}的输入类型
     * @param <B> {@link #function}的输出类型
     */
    public static class Builder<A, B> {

        private final Poller<A, B> poller;

        public Builder(Poller<A, B> poller) {
            this.poller = poller;
        }

        /**
         * 设置{@link Poller}时间相关的配置
         *
         * @param duration {@link Poller#duration}
         * @param interval {@link Poller#interval}
         * @return {@link Builder}
         */
        public Builder<A, B> timing(Duration duration, Duration interval) {
            return timing(Clock.systemDefaultZone(), duration, interval);
        }

        /**
         * 设置{@link Poller}时间相关的配置
         *
         * @param clock    {@link Poller#clock}
         * @param duration {@link Poller#duration}
         * @param interval {@link Poller#interval}
         * @return {@link Builder}
         */
        public Builder<A, B> timing(Clock clock, Duration duration, Duration interval) {
            poller.clock = clock;
            poller.duration = duration;
            poller.interval = interval;
            return this;
        }

        /**
         * 设置{@link Poller}函数相关的配置
         *
         * @param input    {@link Poller#input}
         * @param function {@link Poller#function}
         * @return {@link Builder}
         */
        public <F extends PollerFunction<A, B>> Builder<A, B> execute(A input, F function) {
            poller.input = input;
            poller.function = function;
            return this;
        }

        /**
         * 设置{@link Poller}退出轮询的检查条件
         *
         * @param predicate {@link Poller#predicate}
         * @return {@link Builder}
         */
        public Builder<A, B> predicate(Predicate<B> predicate) {
            poller.predicate = predicate;
            return this;
        }

        /**
         * 设置{@link Poller}轮询期间线程睡眠相关的配置
         *
         * @param sleeper {@link Poller#sleeper}
         * @return {@link Builder}
         * @see Sleeper
         */
        public Builder<A, B> sleeper(Sleeper sleeper) {
            poller.sleeper = sleeper;
            return this;
        }

        /**
         * 设置{@link Poller}轮询超时时的配置
         *
         * @param timeoutAction {@link Poller#timeoutAction}
         * @return {@link Builder}
         */
        public Builder<A, B> onTimeout(Runnable timeoutAction) {
            poller.timeoutAction = timeoutAction;
            return this;
        }

        /**
         * 设置{@link Poller}函数执行期间应该忽略的异常
         *
         * @param exceptions {@link Poller#ignoredExceptions}
         * @return {@link Builder}
         */
        public Builder<A, B> ignoreExceptions(List<Class<? extends Throwable>> exceptions) {
            poller.ignoredExceptions.addAll(exceptions);
            return this;
        }

        /**
         * 设置{@link Poller}函数执行期间应该忽略的异常
         *
         * @param exceptions {@link Poller#ignoredExceptions}
         * @return {@link Builder}
         */
        @SafeVarargs
        public final Builder<A, B> ignoreExceptions(Class<? extends Throwable>... exceptions) {
            poller.ignoredExceptions.addAll(Arrays.stream(exceptions).toList());
            return this;
        }

        /**
         * @return {@link Poller}
         */
        public Poller<A, B> build() {
            if (poller.clock == null) poller.clock = Clock.systemDefaultZone();
            if (poller.duration == null) poller.duration = Duration.ZERO;
            if (poller.interval == null) poller.interval = Duration.ZERO;
            if (poller.function == null) poller.function = (CallableFunction<A, B>) o -> null;
            if (poller.predicate == null) poller.predicate = b -> true;
            if (poller.sleeper == null) poller.sleeper = duration -> LockSupport.parkNanos(duration.toNanos());
            if (poller.timeoutAction == null) poller.timeoutAction = () -> {
            };
            return poller;
        }
    }

}
