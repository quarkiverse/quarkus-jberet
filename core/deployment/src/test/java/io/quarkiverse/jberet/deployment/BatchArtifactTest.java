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

class BatchArtifactTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ArtifactBatchlet.class)
                    .addAsManifestResource("batch.xml")
                    .addAsManifestResource("batch-artifact.xml", "batch-jobs/batch-artifact.xml"));

    @Inject
    JobOperator jobOperator;

    @Test
    void batchXml() {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("refName", "batchlet");
        jobParameters.setProperty("name", "david");
        long executionId = jobOperator.start("batch-artifact", jobParameters);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });
    }

    @Test
    void named() {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("refName", "artifactBatchlet");
        jobParameters.setProperty("name", "david");
        long executionId = jobOperator.start("batch-artifact", jobParameters);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });
    }
}
