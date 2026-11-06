package io.quarkiverse.jberet.deployment;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.Batchlet;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.schedule.JobSchedule;
import org.jberet.schedule.JobScheduler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * The cron configuration is keyed by the Job XML name, which may differ from the Job id.
 */
public class JobSchedulerCronTest {
    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyBatchlet.class)
                    // The Job id is batchlet-job
                    .addAsManifestResource("batchlet.xml", "batch-jobs/scheduled.xml"))
            .overrideConfigKey("quarkus.jberet.job.scheduled.cron", "* * * * * ?")
            .overrideConfigKey("quarkus.jberet.job.scheduled.params.name", "david");

    @Inject
    JobScheduler jobScheduler;
    @Inject
    JobOperator jobOperator;

    @Named("batchlet")
    @Dependent
    public static class DummyBatchlet implements Batchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }

        @Override
        public void stop() {
        }
    }

    @Test
    void cronWithJobIdDifferentFromXmlName() {
        List<JobSchedule> jobSchedules = jobScheduler.getJobSchedules();
        assertEquals(1, jobSchedules.size());
        JobSchedule jobSchedule = jobSchedules.get(0);
        assertEquals("scheduled", jobSchedule.getJobScheduleConfig().getJobName());
        assertEquals("david", jobSchedule.getJobScheduleConfig().getJobParameters().getProperty("name"));

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> !jobSchedule.getJobExecutionIds().isEmpty() && BatchStatus.COMPLETED
                        .equals(jobOperator.getJobExecution(jobSchedule.getJobExecutionIds().get(0)).getBatchStatus()));
    }
}
