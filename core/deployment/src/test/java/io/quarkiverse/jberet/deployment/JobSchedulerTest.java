package io.quarkiverse.jberet.deployment;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.schedule.JobSchedule;
import org.jberet.schedule.JobScheduleConfigBuilder;
import org.jberet.schedule.JobScheduler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusExtensionTest;

public class JobSchedulerTest {
    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyBatchlet.class)
                    .addAsManifestResource("batchlet.xml", "batch-jobs/batchlet.xml"));

    @Inject
    JobScheduler jobScheduler;
    @Inject
    Scheduler scheduler;
    @Inject
    JobOperator jobOperator;

    @Named("batchlet")
    @Dependent
    public static class DummyBatchlet implements Batchlet {
        @Inject
        @BatchProperty(name = "name")
        String name;

        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }

        @Override
        public void stop() {
        }
    }

    @Test
    void singleShot() throws Exception {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("name", "david");
        JobSchedule jobSchedule = jobScheduler.schedule(JobScheduleConfigBuilder.newInstance()
                .jobName("batchlet")
                .jobParameters(jobParameters)
                .initialDelay(0)
                .build());

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> jobSchedule.getJobExecutionIds().size() == 1 && BatchStatus.COMPLETED
                        .equals(jobOperator.getJobExecution(jobSchedule.getJobExecutionIds().get(0)).getBatchStatus()));

        assertNull(scheduler.getScheduledJob(jobSchedule.getId()));
        assertNull(jobScheduler.getJobSchedule(jobSchedule.getId()));
        assertFalse(jobScheduler.cancel(jobSchedule.getId()));

        // Make sure the schedule does not fire again
        Thread.sleep(2000);
        assertEquals(1, jobSchedule.getJobExecutionIds().size());
    }

    @Test
    void cancel() {
        JobSchedule jobSchedule = jobScheduler.schedule(JobScheduleConfigBuilder.newInstance()
                .jobName("batchlet")
                .initialDelay(60)
                .build());
        assertEquals(jobSchedule, jobScheduler.getJobSchedule(jobSchedule.getId()));

        assertTrue(jobScheduler.cancel(jobSchedule.getId()));
        assertNull(scheduler.getScheduledJob(jobSchedule.getId()));
        assertNull(jobScheduler.getJobSchedule(jobSchedule.getId()));
        assertFalse(jobScheduler.cancel(jobSchedule.getId()));
    }

    @Test
    void delete() {
        JobSchedule jobSchedule = jobScheduler.schedule(JobScheduleConfigBuilder.newInstance()
                .jobName("batchlet")
                .initialDelay(60)
                .build());

        jobScheduler.delete(jobSchedule.getId());
        assertNull(scheduler.getScheduledJob(jobSchedule.getId()));
        assertNull(jobScheduler.getJobSchedule(jobSchedule.getId()));
    }

    @Test
    void afterDelay() {
        assertThrows(UnsupportedOperationException.class, () -> jobScheduler.schedule(JobScheduleConfigBuilder.newInstance()
                .jobName("batchlet")
                .afterDelay(1)
                .build()));
    }
}
