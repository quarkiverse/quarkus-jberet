package io.quarkiverse.jberet.it.file;

import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.HttpServer;

@QuarkusTest
class FileItemTest {
    HttpServer httpServer;

    @Test
    void fileProperties(@TempDir Path tempDir) throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        Path outputFile = tempDir.resolve("auctions-output.csv");
        Properties properties = new Properties();
        properties.setProperty("outputFile", outputFile.toString());
        JobExecutionEntity jobExecution = batchClient.startJob("file-properties", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        assertEquals(8, Files.readAllLines(outputFile).size());
    }

    @Test
    void fileBeans(@TempDir Path tempDir) throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        Path outputFile = tempDir.resolve("auctions-output.csv");
        Properties properties = new Properties();
        properties.setProperty("outputFile", outputFile.toString());
        JobExecutionEntity jobExecution = batchClient.startJob("file-beans", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        assertEquals(8, Files.readAllLines(outputFile).size());
    }
}
