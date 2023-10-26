package io.quarkiverse.jberet.runtime;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jberet.schedule.JobSchedule;
import org.jberet.schedule.JobScheduleConfig;
import org.jberet.schedule.JobScheduler;

import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;

public class QuarkusJobScheduler extends JobScheduler {
    private final ScheduledExecutorService executorService;
    private final ConcurrentMap<String, JobSchedule> schedules = new ConcurrentHashMap<>();
    private final AtomicInteger ids = new AtomicInteger(1);

    public QuarkusJobScheduler() {
        this(Executors.newSingleThreadScheduledExecutor());
    }

    public QuarkusJobScheduler(final ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public JobSchedule schedule(final JobScheduleConfig scheduleConfig) {
        JobSchedule jobSchedule = new JobSchedule(String.valueOf(ids.getAndIncrement()), scheduleConfig);
        JobTask task = new JobTask(jobSchedule);

        if (scheduleConfig.getInterval() <= 0 && scheduleConfig.getAfterDelay() <= 0) {
            executorService.schedule(task, scheduleConfig.getInitialDelay(), timeUnit);
        } else if (scheduleConfig.getInterval() > 0) {
            executorService.scheduleAtFixedRate(
                    task, scheduleConfig.getInitialDelay(), scheduleConfig.getInterval(), timeUnit);
        } else {
            executorService.scheduleWithFixedDelay(
                    task, scheduleConfig.getInitialDelay(), scheduleConfig.getAfterDelay(), timeUnit);
        }
        schedules.put(jobSchedule.getId(), jobSchedule);
        return jobSchedule;
    }

    public JobSchedule schedule(final JobScheduleConfig scheduleConfig, final Cron cron) {
        JobSchedule jobSchedule = new JobSchedule(String.valueOf(ids.getAndIncrement()), scheduleConfig);
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(ZonedDateTime.now().truncatedTo(SECONDS));
        if (nextExecution.isPresent()) {
            CronJobTask task = new CronJobTask(jobSchedule, executionTime, nextExecution.get());
            executorService.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
        }
        schedules.put(jobSchedule.getId(), jobSchedule);
        return jobSchedule;
    }

    @Override
    public List<JobSchedule> getJobSchedules() {
        return Collections.unmodifiableList(new ArrayList<>(schedules.values()));
    }

    @Override
    public boolean cancel(final String scheduleId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JobSchedule getJobSchedule(final String scheduleId) {
        return schedules.get(scheduleId);
    }

    static class JobTask implements Runnable {
        private final JobSchedule jobSchedule;

        public JobTask(final JobSchedule jobSchedule) {
            this.jobSchedule = jobSchedule;
        }

        @Override
        public void run() {
            final JobScheduleConfig config = jobSchedule.getJobScheduleConfig();
            if (config.getJobExecutionId() > 0) {
                jobSchedule.addJobExecutionIds(
                        JobScheduler.getJobOperator().restart(config.getJobExecutionId(), config.getJobParameters()));
            } else {
                jobSchedule.addJobExecutionIds(
                        JobScheduler.getJobOperator().start(config.getJobName(), config.getJobParameters()));
            }
        }
    }

    static class CronJobTask extends JobTask {
        private final ExecutionTime executionTime;
        private transient ZonedDateTime nextExecution;

        public CronJobTask(final JobSchedule jobSchedule, final ExecutionTime executionTime,
                final ZonedDateTime nextExecution) {
            super(jobSchedule);
            this.executionTime = executionTime;
            this.nextExecution = nextExecution;
        }

        @Override
        public void run() {
            ZonedDateTime now = ZonedDateTime.now().truncatedTo(SECONDS);
            if (nextExecution != null && now.isAfter(nextExecution)) {
                nextExecution = executionTime.nextExecution(now).orElse(null);
                super.run();
            }
        }
    }
}
