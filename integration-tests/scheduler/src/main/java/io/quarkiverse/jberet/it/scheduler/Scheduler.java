package io.quarkiverse.jberet.it.scheduler;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jberet.schedule.JobScheduleConfig;
import org.jberet.schedule.JobScheduleConfigBuilder;
import org.jberet.schedule.JobScheduler;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class Scheduler {
    @Inject
    JobScheduler jobScheduler;

    void onStart(@Observes StartupEvent startupEvent) {
        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName("scheduler")
                .initialDelay(0)
                .build();

        jobScheduler.schedule(scheduleConfig);
    }
}
