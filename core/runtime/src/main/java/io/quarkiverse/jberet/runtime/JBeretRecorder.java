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
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ThreadPoolConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.DefaultValuesConfigSource;
import io.smallrye.config.SmallRyeConfig;

@Recorder
public class JBeretRecorder {
    private final RuntimeValue<JBeretRuntimeConfig> config;
    private final RuntimeValue<ThreadPoolConfig> threadPoolConfig;

    public JBeretRecorder(RuntimeValue<JBeretRuntimeConfig> config, RuntimeValue<ThreadPoolConfig> threadPoolConfig) {
        this.config = config;
        this.threadPoolConfig = threadPoolConfig;
    }

    public void registerJobs(List<Job> jobs, BeanContainer beanContainer) {
        JobsProducer jobsProducer = beanContainer.beanInstance(JobsProducer.class);
        jobs.addAll(jobsProducer.getJobs());
        JBeretDataHolder.registerJobs(jobs);
    }

    public void init(final BeanContainer beanContainer) {
        ManagedExecutor managedExecutor = beanContainer.beanInstance(ManagedExecutor.class);
        TransactionManager transactionManager = beanContainer.beanInstance(TransactionManager.class);
        JobRepository jobRepository = beanContainer.beanInstance(JobRepository.class);
        JobExecutor quarkusJobExecutor = new QuarkusJobExecutor(managedExecutor, config.getValue(),
                threadPoolConfig.getValue());
        BatchEnvironment batchEnvironment = new QuarkusBatchEnvironment(jobRepository, quarkusJobExecutor, transactionManager);

        JobProcessor jobProcessor = new JobProcessorBuilder().stepConsumer(new SetTransactionTimeout()).build();
        JBeretDataHolder.JBeretData data = JBeretDataHolder.getData();
        data.getJobs().forEach(jobProcessor::processJob);

        QuarkusJobOperator operator = new QuarkusJobOperator(config.getValue(), batchEnvironment, data.getJobs());
        JobOperatorContext operatorContext = JobOperatorContext.create(operator);
        JobOperatorContext.setJobOperatorContextSelector(() -> operatorContext);

        beanContainer.beanInstance(JobScheduler.class);
        JobScheduler.getJobScheduler(QuarkusJobScheduler.Delegate.class, new ConcurrentHashMap<>(), null);
    }

    private static class SetTransactionTimeout implements Consumer<Step> {
        private static final String JAKARTA_TRANSACTION_TIMEOUT = "jakarta.transaction.global.timeout";
        private static final String QUARKUS_TRANSACTION_TIMEOUT = "quarkus.transaction-manager.default-transaction-timeout";

        @Override
        public void accept(Step step) {
            Properties properties = step.getProperties();
            String globalTimeout = properties != null ? properties.get(JAKARTA_TRANSACTION_TIMEOUT) : null;
            if (globalTimeout == null) {
                SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
                ConfigValue configValue = config.getConfigValue(QUARKUS_TRANSACTION_TIMEOUT);
                if (configValue.getValue() != null
                        && !DefaultValuesConfigSource.NAME.equals(configValue.getConfigSourceName())) {
                    Duration transactionTimeout = config.getValue(QUARKUS_TRANSACTION_TIMEOUT, Duration.class);
                    Properties overrideProperties = new Properties();
                    overrideProperties.getNameValues().put(JAKARTA_TRANSACTION_TIMEOUT,
                            String.valueOf(transactionTimeout.toSeconds()));
                    step.setProperties(overrideProperties);
                }
            }
        }
    }
}
