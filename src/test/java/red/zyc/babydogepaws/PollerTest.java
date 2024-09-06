package red.zyc.babydogepaws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.zyc.babydogepaws.common.poller.CallableFunction;
import red.zyc.babydogepaws.common.poller.Poller;
import red.zyc.babydogepaws.common.poller.RunnableFunction;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static red.zyc.babydogepaws.common.poller.Poller.throwingRunnable;

/**
 * @author allurx
 */
public class PollerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollerTest.class);
    private static final ScheduledThreadPoolExecutor e = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        run();
        //call();
    }

    static void run() {
        Poller.<Void, Void>builder()
                .timing(Duration.ofSeconds(10), Duration.ofMillis(500))
                .<RunnableFunction<Void>>execute(null, o -> System.out.println(LocalDateTime.now()))
                .predicate(o -> false)
                .onTimeout(throwingRunnable(() -> new RuntimeException("RuntimeException")))
                .build()
                .poll();
    }

    static void call() {
        AtomicInteger num = new AtomicInteger(1);
        Poller.<AtomicInteger, Integer>builder()
                .timing(Duration.ofSeconds(10), Duration.ofMillis(500))
                .<CallableFunction<AtomicInteger, Integer>>execute(num, i -> {
                    System.out.println(num.get());
                    return num.getAndIncrement();
                })
                .predicate(o -> o == 12)
                .onTimeout(throwingRunnable(() -> new RuntimeException("RuntimeException")))
                .build()
                .poll();
    }

}
