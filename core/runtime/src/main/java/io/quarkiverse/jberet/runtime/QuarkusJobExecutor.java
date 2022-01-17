package io.quarkiverse.jberet.runtime;

import io.quarkus.runtime.ThreadPoolConfig;
import org.jberet.spi.JobExecutor;
import org.wildfly.common.cpu.ProcessorInfo;

import java.util.OptionalInt;
import java.util.concurrent.Executor;

class QuarkusJobExecutor extends JobExecutor {
    private final OptionalInt maxThreads;
    private final int cpuPoolSize = Math.max(8 * ProcessorInfo.availableProcessors(), 200);

    public QuarkusJobExecutor(Executor delegate, final ThreadPoolConfig threadPoolConfig) {
        super(delegate);
        this.maxThreads = threadPoolConfig.maxThreads;
    }

    @Override
    protected int getMaximumPoolSize() {
        return maxThreads.orElse(cpuPoolSize);
    }
}