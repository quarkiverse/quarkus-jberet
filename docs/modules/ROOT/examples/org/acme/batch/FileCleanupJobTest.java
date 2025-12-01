package org.acme.batch;

import static org.awaitility.Awaitility.await;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class FileCleanupJobTest {
    @Inject
    JobOperator jobOperator;

    @Test
    void fileCleanup() {
        Properties properties = new Properties();
        properties.setProperty("directory", "/tmp/batch");
        properties.setProperty("daysToKeep", "30");
        long executionId = jobOperator.start("fileCleanupJob", properties); // <1>

        await().atMost(5, TimeUnit.SECONDS) // <2>
                .until(() -> {
                    JobExecution jobExecution = jobOperator.getJobExecution(executionId);
                    return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus()); // <3>
                });
    }
}
