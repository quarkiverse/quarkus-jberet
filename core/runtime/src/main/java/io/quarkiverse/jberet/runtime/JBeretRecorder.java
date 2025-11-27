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
import org.jberet.schedule.JobScheduleConfig;
import org.jberet.schedule.JobScheduleConfigBuilder;
import org.jberet.schedule.JobScheduler;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobExecutor;
import org.jberet.spi.JobOperatorContext;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import io.quarkiverse.jberet.runtime.JBeretConfig.JobConfig;
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
    private final JBeretConfig config;
    private final RuntimeValue<ThreadPoolConfig> threadPoolConfig;

    public JBeretRecorder(JBeretConfig config, RuntimeValue<ThreadPoolConfig> threadPoolConfig) {
        this.config = config;
        this.threadPoolConfig = threadPoolConfig;
    }

    public void registerJobs(List<Job> jobs, BeanContainer beanContainer) {
        JobsProducer jobsProducer = beanContainer.beanInstance(JobsProducer.class);
        jobs.addAll(jobsProducer.getJobs());
        JBeretDataHolder.registerJobs(jobs);
    }

    public void initJobOperator(final BeanContainer beanContainer) {
        ManagedExecutor managedExecutor = beanContainer.beanInstance(ManagedExecutor.class);
        TransactionManager transactionManager = beanContainer.beanInstance(TransactionManager.class);
        JobRepository jobRepository = beanContainer.beanInstance(JobRepository.class);
        JobExecutor quarkusJobExecutor = new QuarkusJobExecutor(managedExecutor, threadPoolConfig.getValue(), config);
        BatchEnvironment batchEnvironment = new QuarkusBatchEnvironment(jobRepository, quarkusJobExecutor, transactionManager);

        JobProcessor jobProcessor = new JobProcessorBuilder().stepConsumer(new SetTransactionTimeout()).build();
        JBeretDataHolder.JBeretData data = JBeretDataHolder.getData();
        data.getJobs().forEach(jobProcessor::processJob);

        QuarkusJobOperator operator = new QuarkusJobOperator(config, batchEnvironment, data.getJobs());
        JobOperatorContext operatorContext = JobOperatorContext.create(operator);
        JobOperatorContext.setJobOperatorContextSelector(() -> operatorContext);
    }

    public void initScheduler() {
        if (config.job().values().stream().noneMatch(jobConfig -> jobConfig.cron().isPresent())) {
            return;
        }

        QuarkusJobScheduler jobScheduler = (QuarkusJobScheduler) JobScheduler.getJobScheduler(QuarkusJobScheduler.class,
                new ConcurrentHashMap<>(), null);

        // TODO - Record Cron
        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        for (Job job : JBeretDataHolder.getData().getJobs()) {
            JobConfig jobConfig = config.job().get(job.getJobXmlName());
            if (jobConfig != null && jobConfig.cron().isPresent()) {
                Cron cron = parser.parse(jobConfig.cron().get());
                java.util.Properties jobParameters = new java.util.Properties();
                if (jobConfig.params() != null && !jobConfig.params().isEmpty()) {
                    jobParameters.putAll(jobConfig.params());
                }

                JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                        .jobName(job.getJobXmlName())
                        .jobParameters(jobParameters).build();

                jobScheduler.schedule(scheduleConfig, cron);
            }
        }
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
