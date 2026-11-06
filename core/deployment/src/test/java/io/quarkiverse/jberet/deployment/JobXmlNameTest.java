package io.quarkiverse.jberet.deployment;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.jberet.runtime.QuarkusJobLauncher;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A Job XML name may differ from the Job id. The Job is started and configured with the Job XML name, while the Job
 * repository stores the instances and executions with the Job id.
 */
public class JobXmlNameTest {
    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FailOnceBatchlet.class)
                    // The Job id is job-id
                    .addAsManifestResource("job-xml-name.xml", "batch-jobs/job-xml-name.xml"))
            .overrideConfigKey("quarkus.jberet.job.job-xml-name.params.name", "config");

    @Inject
    JobOperator jobOperator;
    @Inject
    QuarkusJobLauncher jobLauncher;

    @BeforeEach
    void setUp() {
        FailOnceBatchlet.fail = true;
        FailOnceBatchlet.name = null;
    }

    @Test
    void launcherRestartsFailedExecution() throws Exception {
        JobExecution failed = jobLauncher.run("job-xml-name", new Properties());
        assertEquals(BatchStatus.FAILED, failed.getBatchStatus());

        JobExecution restarted = jobLauncher.run("job-xml-name", new Properties());
        assertEquals(BatchStatus.COMPLETED, restarted.getBatchStatus());
        assertEquals(instanceId(failed), instanceId(restarted));
    }

    @Test
    void restartWithConfiguredParameters() {
        Properties parameters = new Properties();
        parameters.setProperty("name", "start");
        long executionId = jobOperator.start("job-xml-name", parameters);
        awaitStatus(executionId, BatchStatus.FAILED);
        assertEquals("start", FailOnceBatchlet.name);

        // The configured parameters are added to the restart parameters, and override the original ones
        long restartId = jobOperator.restart(executionId, new Properties());
        awaitStatus(restartId, BatchStatus.COMPLETED);
        assertEquals("config", FailOnceBatchlet.name);
    }

    private void awaitStatus(final long executionId, final BatchStatus status) {
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> status.equals(jobOperator.getJobExecution(executionId).getBatchStatus()));
    }

    private long instanceId(final JobExecution execution) {
        return jobOperator.getJobInstance(execution.getExecutionId()).getInstanceId();
    }

    @Named("failOnceBatchlet")
    @Dependent
    public static class FailOnceBatchlet implements Batchlet {
        static volatile boolean fail;
        static volatile String name;

        @Inject
        @BatchProperty(name = "name")
        String nameProperty;

        @Override
        public String process() {
            name = nameProperty;
            if (fail) {
                fail = false;
                throw new IllegalStateException("Fail once");
            }
            return BatchStatus.COMPLETED.toString();
        }

        @Override
        public void stop() {
        }
    }
}
