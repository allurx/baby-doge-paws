package red.zyc.babydogepaws.common.poller;

import java.time.Duration;

/**
 * 如何让线程睡眠，用户可以实现这个接口以便更好的控制逻辑
 *
 * @author allurx
 */
public interface Sleeper {

    /**
     * 让线程睡眠指定的时长
     *
     * @param duration 线程睡眠的时长
     */
    void sleep(Duration duration);
}
