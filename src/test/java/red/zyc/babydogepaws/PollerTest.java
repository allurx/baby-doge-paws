package red.zyc.babydogepaws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.zyc.babydogepaws.common.poller.Poller;
import red.zyc.babydogepaws.common.poller.RunnableFunction;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static red.zyc.babydogepaws.common.poller.Poller.throwingRunnable;

/**
 * @author allurx
 */
public class PollerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollerTest.class);
    private static final ScheduledThreadPoolExecutor e = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        int i = 0;
        //e.scheduleWithFixedDelay(() -> LOGGER.info("2"), 0, 1, TimeUnit.SECONDS);
        Poller.<Integer, Void>builder()
                .timing(Duration.ofSeconds(10), Duration.ofMillis(500))
                .<RunnableFunction<Integer>>execute(1, input -> LOGGER.info((++input).toString()))
                .predicate(o -> false)
                .onTimeout(throwingRunnable(() -> new RuntimeException("RuntimeException")))
                .build()
                .poll();


    }

}
