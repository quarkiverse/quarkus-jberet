package io.quarkiverse.jberet.runtime;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.transaction.TransactionManager;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jberet.job.model.Job;
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
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ThreadPoolConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JBeretRecorder {
    public void registerJobs(List<Job> jobs, BeanContainer beanContainer) {
        JobsProducer jobsProducer = beanContainer.beanInstance(JobsProducer.class);
        jobs.addAll(jobsProducer.getJobs());
        JBeretDataHolder.registerJobs(jobs);
    }

    public void initJobOperator(final JBeretConfig config, final ThreadPoolConfig threadPoolConfig,
            final BeanContainer beanContainer) {
        ManagedExecutor managedExecutor = beanContainer.beanInstance(ManagedExecutor.class);
        TransactionManager transactionManager = beanContainer.beanInstance(TransactionManager.class);

        JobRepository jobRepository = beanContainer.beanInstance(
                JobRepository.class);

        JobExecutor quarkusJobExecutor = new QuarkusJobExecutor(managedExecutor, threadPoolConfig, config);

        JBeretDataHolder.JBeretData data = JBeretDataHolder.getData();

        BatchEnvironment batchEnvironment = new QuarkusBatchEnvironment(
                jobRepository,
                quarkusJobExecutor,
                transactionManager,
                data);

        QuarkusJobOperator operator = new QuarkusJobOperator(
                config,
                batchEnvironment,
                data.getJobs());
        JobOperatorContext operatorContext = JobOperatorContext.create(operator);
        JobOperatorContext.setJobOperatorContextSelector(() -> operatorContext);
    }

    public void initScheduler(final JBeretConfig config) {
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
}
