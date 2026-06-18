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
            jobDefinition.setDelayed(scheduleConfig.getInitialDelay() + "m");
        } else if (scheduleConfig.getInterval() > 0) {
            jobDefinition.setDelayed(scheduleConfig.getInitialDelay() + "m");
            jobDefinition.setInterval(scheduleConfig.getInterval() + "m");
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

    static String toCronExpression(final ScheduleExpression scheduleExpression) {
        String dayOfMonth = scheduleExpression.getDayOfMonth();
        String dayOfWeek = toDayOfWeek(scheduleExpression.getDayOfWeek());

        if (!"*".equals(dayOfWeek) && !"?".equals(dayOfWeek)) {
            dayOfMonth = "?";
        } else if (!"*".equals(dayOfMonth) && !"?".equals(dayOfMonth)) {
            dayOfWeek = "?";
        } else {
            dayOfWeek = "?";
        }

        return scheduleExpression.getSecond() + " " +
                scheduleExpression.getMinute() + " " +
                scheduleExpression.getHour() + " " +
                dayOfMonth + " " +
                scheduleExpression.getMonth() + " " +
                dayOfWeek + " " +
                scheduleExpression.getYear();
    }

    // EJB: 0=Sun, 1=Mon, ..., 6=Sat, 7=Sun
    // Quartz: 1=Sun, 2=Mon, ..., 7=Sat
    static String toDayOfWeek(String dayOfWeek) {
        if (dayOfWeek == null || dayOfWeek.equals("*") || dayOfWeek.equals("?")) {
            return dayOfWeek;
        }

        StringBuilder result = new StringBuilder();
        for (String listPart : dayOfWeek.split(",")) {
            if (!result.isEmpty()) {
                result.append(",");
            }
            if (listPart.contains("/")) {
                String[] parts = listPart.split("/", 2);
                result.append(convertDayOfWeek(parts[0])).append("/").append(parts[1]);
            } else if (listPart.contains("-")) {
                String[] parts = listPart.split("-", 2);
                result.append(convertDayOfWeek(parts[0])).append("-").append(convertDayOfWeek(parts[1]));
            } else {
                result.append(convertDayOfWeek(listPart));
            }
        }
        return result.toString();
    }

    static String convertDayOfWeek(String token) {
        try {
            int value = Integer.parseInt(token.trim());
            int convertedValue = (value == 0 || value == 7) ? 1 : value + 1;
            return String.valueOf(convertedValue);
        } catch (NumberFormatException e) {
            return token;
        }
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
