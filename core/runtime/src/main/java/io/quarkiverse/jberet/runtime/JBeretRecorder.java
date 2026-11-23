package io.quarkiverse.jberet.runtime;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import jakarta.transaction.TransactionManager;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jberet.job.model.Job;
import org.jberet.job.model.Properties;
import org.jberet.job.model.Step;
import org.jberet.repository.JobRepository;
import org.jberet.schedule.JobScheduler;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobExecutor;
import org.jberet.spi.JobOperatorContext;

import io.quarkiverse.jberet.runtime.JobProcessor.JobProcessorBuilder;
import io.quarkiverse.jberet.runtime.telemetry.JBeretInstrumenterTracer;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ThreadPoolConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.scheduler.runtime.SchedulerRuntimeConfig;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.DefaultValuesConfigSource;
import io.smallrye.config.SmallRyeConfig;

/**
 * The {@link Recorder} is only kept for {@link #registerJobs(List, BeanContainer)}, because the parsed {@link Job}
 * graph is recorded with non-default constructors, and cannot be captured by a service lambda.
 */
@Recorder
public class JBeretRecorder {
    public void registerJobs(List<Job> jobs, BeanContainer beanContainer) {
        JobsProducer jobsProducer = beanContainer.beanInstance(JobsProducer.class);
        jobs.addAll(jobsProducer.getJobs());
        JBeretDataHolder.registerJobs(jobs);
    }

    public static void init(
            final BeanContainer beanContainer,
            final JBeretRuntimeConfig config,
            final ThreadPoolConfig threadPoolConfig,
            final SchedulerRuntimeConfig schedulerConfig,
            final boolean tracingAvailable) {
        boolean tracing = tracingAvailable && config.telemetry().tracesEnabled();
        ManagedExecutor managedExecutor = beanContainer.beanInstance(ManagedExecutor.class);
        TransactionManager transactionManager = beanContainer.beanInstance(TransactionManager.class);
        JobRepository jobRepository = beanContainer.beanInstance(QuarkusJobRepository.class);
        JobExecutor quarkusJobExecutor = new QuarkusJobExecutor(managedExecutor, config, threadPoolConfig);
        BatchEnvironment batchEnvironment = new QuarkusBatchEnvironment(
                jobRepository, quarkusJobExecutor, transactionManager, tracing);

        JobProcessor jobProcessor = new JobProcessorBuilder().stepConsumer(new SetTransactionTimeout()).build();
        JBeretDataHolder.JBeretData data = JBeretDataHolder.getData();
        data.getJobs().forEach(jobProcessor::processJob);
        if (tracing) {
            JobProcessor tracingListeners = JBeretInstrumenterTracer.listeners();
            data.getJobs().forEach(tracingListeners::processJob);
        }

        QuarkusJobOperator operator = new QuarkusJobOperator(config, batchEnvironment, data.getJobs());
        JobOperatorContext operatorContext = JobOperatorContext.create(operator);
        JobOperatorContext.setJobOperatorContextSelector(() -> operatorContext);

        if (schedulerConfig.enabled()) {
            beanContainer.beanInstance(JobScheduler.class);
            JobScheduler.getJobScheduler(QuarkusJobScheduler.Delegate.class, new ConcurrentHashMap<>(), null);
        }
    }

    private static class SetTransactionTimeout implements Consumer<Step> {
        private static final String JAKARTA_TRANSACTION_TIMEOUT = "jakarta.transaction.global.timeout";
        private static final String QUARKUS_TRANSACTION_TIMEOUT = "quarkus.transaction-manager.default-transaction-timeout";

        @Override
        public void accept(Step step) {
            Properties properties = step.getProperties();
            if (properties.get(JAKARTA_TRANSACTION_TIMEOUT) == null) {
                SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
                ConfigValue configValue = config.getConfigValue(QUARKUS_TRANSACTION_TIMEOUT);
                if (configValue.getValue() != null
                        && !DefaultValuesConfigSource.NAME.equals(configValue.getConfigSourceName())) {
                    Duration transactionTimeout = config.getValue(QUARKUS_TRANSACTION_TIMEOUT, Duration.class);
                    properties.getNameValues().put(JAKARTA_TRANSACTION_TIMEOUT,
                            String.valueOf(transactionTimeout.toSeconds()));
                }
            }
        }
    }
}
