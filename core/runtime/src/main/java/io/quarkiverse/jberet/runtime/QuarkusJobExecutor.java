package io.quarkiverse.jberet.runtime;

import java.util.concurrent.Executor;

import org.jberet.spi.JobExecutor;
import org.wildfly.common.cpu.ProcessorInfo;

import io.quarkus.logging.Log;
import io.quarkus.runtime.ThreadPoolConfig;

class QuarkusJobExecutor extends JobExecutor {
    private final int maxPoolSize;

    public QuarkusJobExecutor(Executor delegate, final ThreadPoolConfig threadPoolConfig, final JBeretConfig config) {
        super(delegate);
        this.maxPoolSize = config.maxAsync().map(maxAsync -> {
            if (maxAsync < 1) {
                Log.error("max-async value must be 1 or greater if set.");
                return null;
            }
            return maxAsync + 1; // Adapt for the fact that JBeret requires one thread for coordination.
        }).orElse(threadPoolConfig.maxThreads.orElse(Math.max(8 * ProcessorInfo.availableProcessors(), 200)));
    }

    @Override
    protected int getMaximumPoolSize() {
        return maxPoolSize;
    }
}
