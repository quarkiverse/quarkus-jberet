package io.quarkiverse.jberet.runtime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.jberet.schedule.ExecutorSchedulerImpl;

public class QuarkusJobScheduler extends ExecutorSchedulerImpl {
    public QuarkusJobScheduler() {
        super();
    }

    public QuarkusJobScheduler(final ScheduledExecutorService executorService) {
        super(new ConcurrentHashMap<>(), executorService);
    }
}
