package red.zyc.babydogepaws.common;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author allurx
 */
public class NamedThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final boolean virtual;

    public NamedThreadFactory(String namePrefix, boolean virtual) {
        this.namePrefix = namePrefix;
        this.virtual = virtual;
    }

    public Thread newThread(Runnable r) {
        return virtual ? Thread.ofVirtual()
                .name(namePrefix + "-" + this.threadNumber.getAndIncrement())
                .unstarted(r) :
                Thread.ofPlatform()
                        .name(namePrefix + "-" + this.threadNumber.getAndIncrement())
                        .unstarted(r);
    }
}
