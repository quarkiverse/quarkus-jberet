package io.quarkiverse.jberet.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.batch.operations.JobOperator;
import jakarta.ejb.ScheduleExpression;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jberet.schedule.JobSchedule;
import org.jberet.schedule.JobScheduleConfig;
import org.jberet.schedule.JobScheduleConfigBuilder;
import org.jberet.schedule.JobScheduler;

import io.quarkiverse.jberet.runtime.JBeretRuntimeConfig.JobConfig;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Scheduler.JobDefinition;
import io.quarkus.scheduler.Trigger;

@ApplicationScoped
public class QuarkusJobScheduler extends JobScheduler {
    private static final String TRIGGER_ID_PREFIX = "quarkus-jberet-";

    private final AtomicInteger ids = new AtomicInteger(1);
    private final ConcurrentMap<String, JobSchedule> jobSchedules = new ConcurrentHashMap<>();

    @Inject
    JBeretRuntimeConfig config;
    @Inject
    InjectableInstance<Scheduler> scheduler;
    @Inject
    JobOperator jobOperator;

    @Startup
    void schedule() {
        for (String jobName : jobOperator.getJobNames()) {
            JobConfig jobConfig = config.job().get(jobName);
            if (jobConfig.cron().isPresent()) {
                JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                        .jobName(jobName)
                        .jobParameters(jobConfig.paramsAsProperties())
                        .scheduleExpression(new ScheduleExpression())
                        .build();
                schedule(scheduleConfig);
            }
        }
    }

    @Override
    public JobSchedule schedule(final JobScheduleConfig scheduleConfig) {
        String scheduleId = generateScheduleId(scheduleConfig.getJobName());
        JobSchedule jobSchedule = new JobSchedule(scheduleId, scheduleConfig);
        JobDefinition<?> jobDefinition = scheduler.getActive().newJob(scheduleId);
        if (scheduleConfig.getScheduleExpression() != null) {
            JobConfig jobConfig = config.job().get(scheduleConfig.getJobName());
            if (jobConfig.cron().isPresent()) {
                jobDefinition.setCron(jobConfig.cron().get());
            } else {
                jobDefinition.setCron(toCronExpression(scheduleConfig.getScheduleExpression()));
            }
        } else if (scheduleConfig.getInterval() <= 0 && scheduleConfig.getAfterDelay() <= 0) {
            jobDefinition.setDelayed(String.valueOf(scheduleConfig.getInitialDelay()));
        } else if (scheduleConfig.getInterval() > 0) {
            jobDefinition.setDelayed(String.valueOf(scheduleConfig.getInitialDelay()));
            jobDefinition.setInterval(String.valueOf(scheduleConfig.getInterval()));
        } else {
            // TODO - Quarkus Scheduler does not support scheduleWithFixedDelay
            throw new UnsupportedOperationException();
        }
        jobDefinition.setTask(new Consumer<ScheduledExecution>() {
            @Override
            public void accept(ScheduledExecution scheduledExecution) {
                JobScheduleConfig jobScheduleConfig = jobSchedule.getJobScheduleConfig();
                if (jobScheduleConfig.getJobExecutionId() == 0) {
                    jobSchedule.addJobExecutionIds(
                            jobOperator.start(scheduleConfig.getJobName(), scheduleConfig.getJobParameters()));
                } else {
                    // TODO - setJobExecutionId if the job fails to restart
                    jobSchedule.addJobExecutionIds(
                            jobOperator.restart(jobScheduleConfig.getJobExecutionId(), scheduleConfig.getJobParameters()));
                }
            }
        });
        jobSchedules.computeIfAbsent(scheduleId, new Function<String, JobSchedule>() {
            @Override
            public JobSchedule apply(String key) {
                jobDefinition.schedule();
                return jobSchedule;
            }
        });
        return jobSchedule;
    }

    @Override
    public List<JobSchedule> getJobSchedules() {
        List<JobSchedule> jobSchedules = new ArrayList<>();
        for (Trigger scheduledJob : scheduler.get().getScheduledJobs()) {
            String id = scheduledJob.getId();
            if (id.startsWith(TRIGGER_ID_PREFIX)) {
                JobSchedule jobSchedule = this.jobSchedules.get(id);
                if (jobSchedule != null) {
                    jobSchedules.add(jobSchedule);
                }
            }
        }
        return jobSchedules;
    }

    @Override
    public boolean cancel(final String scheduleId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JobSchedule getJobSchedule(final String scheduleId) {
        for (Trigger scheduledJob : scheduler.get().getScheduledJobs()) {
            if (scheduledJob.getId().equals(scheduleId)) {
                JobSchedule jobSchedule = this.jobSchedules.get(scheduleId);
                if (jobSchedule != null) {
                    return jobSchedule;
                }
            }
        }
        return null;
    }

    private String generateScheduleId(final String jobName) {
        return TRIGGER_ID_PREFIX + jobName + "-" + ids.getAndIncrement();
    }

    private String toCronExpression(final ScheduleExpression scheduleExpression) {
        return scheduleExpression.getSecond() + " " +
                scheduleExpression.getMinute() + " " +
                scheduleExpression.getHour() + " " +
                scheduleExpression.getDayOfMonth() + " " +
                scheduleExpression.getMonth() + " " +
                scheduleExpression.getDayOfWeek() + " " +
                scheduleExpression.getYear();
    }

    public static class Delegate extends JobScheduler {
        JobScheduler delegate;

        public Delegate() {
            InjectableInstance<JobScheduler> jobSchedulers = Arc.container().select(JobScheduler.class);
            delegate = jobSchedulers.isUnsatisfied() ? new NoOpJobScheduler() : jobSchedulers.get();
        }

        @Override
        public String[] getFeatures() {
            return delegate.getFeatures();
        }

        @Override
        public void delete(String scheduleId) {
            delegate.delete(scheduleId);
        }

        @Override
        public JobSchedule schedule(JobScheduleConfig scheduleConfig) {
            return delegate.schedule(scheduleConfig);
        }

        @Override
        public List<JobSchedule> getJobSchedules() {
            return delegate.getJobSchedules();
        }

        @Override
        public boolean cancel(String scheduleId) {
            return delegate.cancel(scheduleId);
        }

        @Override
        public JobSchedule getJobSchedule(String scheduleId) {
            return delegate.getJobSchedule(scheduleId);
        }
    }

    private static class NoOpJobScheduler extends JobScheduler {
        @Override
        public String[] getFeatures() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String scheduleId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JobSchedule schedule(JobScheduleConfig scheduleConfig) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<JobSchedule> getJobSchedules() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean cancel(String scheduleId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JobSchedule getJobSchedule(String scheduleId) {
            throw new UnsupportedOperationException();
        }
    }
}
