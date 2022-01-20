package io.quarkiverse.jberet.runtime;

import java.util.concurrent.Executor;

import org.jberet.spi.JobExecutor;
import org.wildfly.common.cpu.ProcessorInfo;

import io.quarkus.runtime.ThreadPoolConfig;

class QuarkusJobExecutor extends JobExecutor {
    private final int maxPoolSize;

    public QuarkusJobExecutor(Executor delegate, final ThreadPoolConfig threadPoolConfig) {
        super(delegate);
        this.maxPoolSize = threadPoolConfig.maxThreads.orElse(Math.max(8 * ProcessorInfo.availableProcessors(), 200));
    }

    @Override
    protected int getMaximumPoolSize() {
        return maxPoolSize;
    }
}
