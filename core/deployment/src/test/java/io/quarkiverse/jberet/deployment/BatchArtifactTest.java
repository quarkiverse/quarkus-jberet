package io.quarkiverse.jberet.deployment;

import static org.awaitility.Awaitility.await;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BatchArtifactTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ArtifactBatchlet.class)
                    .addAsManifestResource("batch.xml")
                    .addAsManifestResource("batchlet.xml", "batch-jobs/batchlet.xml"));

    @Inject
    JobOperator jobOperator;

    @Test
    public void runBatchletJob() {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("name", "david");
        long executionId = jobOperator.start("batchlet", jobParameters);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });
    }
}
