package red.zyc.babydogepaws.common;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 轮询器
 *
 * @param <A> 轮询器执行函数的输入类型
 * @param <B> 轮询器执行函数的返回类型
 * @author allurx
 */
public class Poller<A, B> {

    /**
     * 轮询的最大时长
     */
    private long duration;

    /**
     * 每次轮询的间隔
     */
    private long interval;

    /**
     * 时间单位
     */
    private TimeUnit timeUnit;

    /**
     * 轮询期间执行的函数
     */
    private Function<A, B> function;

    /**
     * 轮询期间执行函数的输入对象
     */
    private A input;

    /**
     * 轮询结束的条件
     */
    private Predicate<B> predicate;

    private Poller() {
    }

    /**
     * @return duration内满足predicate的function执行结果
     */
    public Optional<B> poll() {
        B r = null;
        long durationNanoTime = TimeUnit.NANOSECONDS.convert(this.duration, this.timeUnit);
        long intervalNanoTime = TimeUnit.NANOSECONDS.convert(this.interval, this.timeUnit);
        long startTime = System.nanoTime();

        while (System.nanoTime() - startTime < durationNanoTime && !this.predicate.test(r = this.function.apply(this.input))) {
            LockSupport.parkNanos(intervalNanoTime);
        }

        return Optional.ofNullable(r);
    }

    /**
     * 获取duration内满足predicate的function执行结果，如果超时则抛出exceptionSupplier提供的{@link Throwable}
     *
     * @param exceptionSupplier {@link Throwable}提供器
     * @param <X>               {@link Throwable}的类型
     * @return duration内满足predicate的function执行结果
     * @throws X Throwable
     */
    public <X extends Throwable> Optional<B> throwWhenMiss(Supplier<? extends X> exceptionSupplier) throws X {
        B r;
        long durationNanoTime = TimeUnit.NANOSECONDS.convert(this.duration, this.timeUnit);
        long intervalNanoTime = TimeUnit.NANOSECONDS.convert(this.interval, this.timeUnit);
        long startTime = System.nanoTime();
        while (System.nanoTime() - startTime < durationNanoTime) {
            if (this.predicate.test(r = this.function.apply(this.input))) {
                return Optional.ofNullable(r);
            }
            LockSupport.parkNanos(intervalNanoTime);
        }
        throw exceptionSupplier.get();
    }

    /**
     * 用来构造Poller的构造器
     *
     * @param <IN>  Poller执行函数的输入类型
     * @param <OUT> Poller执行函数的输出类型
     * @return 构造Poller的构造器
     */
    public static <IN, OUT> PollerBuilder<IN, OUT> builder() {
        return new PollerBuilder<>(new Poller<>());
    }

    /**
     * Poller构造器
     *
     * @param <C> Poller执行函数的输入类型
     * @param <D> Poller执行函数的输出类型
     */
    public static class PollerBuilder<C, D> {

        private final Poller<C, D> poller;

        public PollerBuilder(Poller<C, D> poller) {
            this.poller = poller;
        }

        public PollerBuilder<C, D> duration(long duration) {
            this.poller.duration = duration;
            return this;
        }

        public PollerBuilder<C, D> interval(long interval) {
            this.poller.interval = interval;
            return this;
        }

        public PollerBuilder<C, D> timeUnit(TimeUnit timeUnit) {
            this.poller.timeUnit = timeUnit;
            return this;
        }

        public PollerBuilder<C, D> function(Function<C, D> function) {
            this.poller.function = function;
            return this;
        }

        public PollerBuilder<C, D> input(C input) {
            this.poller.input = input;
            return this;
        }

        public PollerBuilder<C, D> predicate(Predicate<D> predicate) {
            this.poller.predicate = predicate;
            return this;
        }

        public Poller<C, D> build() {
            if (this.poller.duration >= 0L && this.poller.interval >= 0L) {
                Objects.requireNonNull(this.poller.timeUnit);
                Objects.requireNonNull(this.poller.function);
                Objects.requireNonNull(this.poller.input);
                Objects.requireNonNull(this.poller.predicate);
                return this.poller;
            } else {
                throw new IllegalArgumentException("duration or interval is invalid");
            }
        }
    }
}
