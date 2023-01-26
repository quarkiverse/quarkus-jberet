package io.quarkiverse.jberet.runtime;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.transaction.TransactionManager;

import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jberet.job.model.Decision;
import org.jberet.job.model.Flow;
import org.jberet.job.model.InheritableJobElement;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.Properties;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Split;
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
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ThreadPoolConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JBeretRecorder {
    public void registerJobs(List<Job> jobs, Map<String, String> batchArtifactsAliases) {
        JBeretDataHolder.registerJobs(jobs, batchArtifactsAliases);
    }

    public RuntimeValue<ConfigSourceProvider> config() {
        final Set<String> properties = new HashSet<>();
        final List<Job> jobs = JBeretDataHolder.getData().getJobs();
        for (Job job : jobs) {
            addConfigNames(properties, job);

            for (JobElement jobElement : job.getJobElements()) {
                addConfigNames(properties, jobElement);
            }

            for (final InheritableJobElement inheritingJobElement : job.getInheritingJobElements()) {
                addConfigNames(properties, inheritingJobElement);
            }
        }

        return new RuntimeValue<>(new JBeretConfigSourceProvider(properties));
    }

    public void initJobOperator(final JBeretConfig config, final ThreadPoolConfig threadPoolConfig,
            final BeanContainer beanContainer) {
        ManagedExecutor managedExecutor = beanContainer.beanInstance(ManagedExecutor.class);
        TransactionManager transactionManager = beanContainer.beanInstance(TransactionManager.class);

        JBeretRepositoryFactory repositoryFactory = beanContainer.beanInstance(
                JBeretRepositoryFactory.class);

        JobRepository repository = repositoryFactory.apply(config);

        JobExecutor quarkusJobExecutor = new QuarkusJobExecutor(managedExecutor, threadPoolConfig);

        JBeretDataHolder.JBeretData data = JBeretDataHolder.getData();

        BatchEnvironment batchEnvironment = new QuarkusBatchEnvironment(
                repository,
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

    private static void addConfigNames(final Set<String> properties, final JobElement jobElement) {
        if (jobElement instanceof Step) {
            final Step step = (Step) jobElement;
            addConfigNames(properties, step);

            addConfigNames(properties, step.getBatchlet());

            if (step.getChunk() != null) {
                addConfigNames(properties, step.getChunk().getReader());
                addConfigNames(properties, step.getChunk().getProcessor());
                addConfigNames(properties, step.getChunk().getWriter());
                addConfigNames(properties, step.getChunk().getCheckpointAlgorithm());
            }

            if (step.getPartition() != null) {
                addConfigNames(properties, step.getPartition().getMapper());
                addConfigNames(properties, step.getPartition().getCollector());
                addConfigNames(properties, step.getPartition().getAnalyzer());
                addConfigNames(properties, step.getPartition().getReducer());
            }
        }

        if (jobElement instanceof Flow) {
            final Flow flow = (Flow) jobElement;
            addConfigNames(properties, flow);

            for (JobElement flowElement : flow.getJobElements()) {
                addConfigNames(properties, flowElement);
            }
        }

        if (jobElement instanceof Split) {
            final Split split = (Split) jobElement;
            addConfigNames(properties, split.getProperties());
            for (final Flow flow : split.getFlows()) {
                addConfigNames(properties, (JobElement) flow);
            }
        }

        if (jobElement instanceof Decision) {
            final Decision decision = (Decision) jobElement;
            addConfigNames(properties, decision.getProperties());
        }
    }

    private static void addConfigNames(final Set<String> properties, final InheritableJobElement inheritableJobElement) {
        if (inheritableJobElement != null) {
            if (inheritableJobElement.getProperties() != null) {
                addConfigNames(properties, inheritableJobElement.getProperties());
            }

            if (inheritableJobElement.getListeners() != null) {
                for (RefArtifact refArtifact : inheritableJobElement.getListeners().getListeners()) {
                    addConfigNames(properties, refArtifact);
                }
            }
        }
    }

    private static void addConfigNames(final Set<String> properties, final RefArtifact refArtifact) {
        if (refArtifact != null && refArtifact.getProperties() != null) {
            addConfigNames(properties, refArtifact.getProperties());
        }
    }

    private static void addConfigNames(final Set<String> properties, final Properties propertiesHolder) {
        if (propertiesHolder != null) {
            properties.addAll(propertiesHolder.getNameValues().keySet());
        }
    }
}
