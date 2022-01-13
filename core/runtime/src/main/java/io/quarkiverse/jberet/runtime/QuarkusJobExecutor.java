package io.quarkiverse.jberet.runtime;

import com.cronutils.utils.StringUtils;
import org.jberet.spi.JobExecutor;

import java.util.concurrent.Executor;

class QuarkusJobExecutor extends JobExecutor {
    private final JBeretConfig config;

    public QuarkusJobExecutor(Executor delegate, final JBeretConfig config) {
        super(delegate);
        this.config = config;
    }

    @Override
    protected int getMaximumPoolSize() {
        // From io.quarkus.smallrye.context.runtime.SmallRyeContextPropagationRecorder.initializeManagedExecutor and
        // io.smallrye.context.SmallRyeManagedExecutor.newThreadPoolExecutor
        // It is initialized with -1 and fallbacks to Runtime.getRuntime().availableProcessors();
        // Added max_processors config to manually override
        var availableProcessors = Runtime.getRuntime().availableProcessors();
        if (config.max_processors.isPresent() && StringUtils.isNumeric(config.max_processors.get())) {
            try {
                var maxProcessors = Integer.valueOf(config.max_processors.get());
                return maxProcessors < 0 ? availableProcessors : maxProcessors;
            } catch (NumberFormatException ex) {
                return availableProcessors;
            }
        }
        return availableProcessors;
    }
}
