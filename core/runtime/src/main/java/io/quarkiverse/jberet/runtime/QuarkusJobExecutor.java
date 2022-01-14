package io.quarkiverse.jberet.runtime;

import io.quarkus.runtime.ThreadPoolConfig;
import org.jberet.spi.JobExecutor;
import org.wildfly.common.cpu.ProcessorInfo;

import java.util.concurrent.Executor;

class QuarkusJobExecutor extends JobExecutor {
    private final ThreadPoolConfig threadPoolConfig;

    public QuarkusJobExecutor(Executor delegate, final ThreadPoolConfig threadPoolConfig) {
        super(delegate);
        this.threadPoolConfig = threadPoolConfig;
    }

    @Override
    protected int getMaximumPoolSize() {
        // From io.quarkus.smallrye.context.runtime.SmallRyeContextPropagationRecorder.initializeManagedExecutor and
        // io.smallrye.context.SmallRyeManagedExecutor.newThreadPoolExecutor
        // It is initialized with -1 and fallbacks to Runtime.getRuntime().availableProcessors();
        final var cpus = ProcessorInfo.availableProcessors();
        return threadPoolConfig.maxThreads.orElse(Math.max(8 * cpus, 200));
    }
}
