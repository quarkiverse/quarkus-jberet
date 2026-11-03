package io.quarkiverse.jberet.it.file;

import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static jakarta.batch.runtime.BatchStatus.FAILED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    @Test
    void fileRestart(@TempDir Path tempDir) throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        Path outputFile = tempDir.resolve("auctions-output.csv");
        Properties properties = new Properties();
        properties.setProperty("outputFile", outputFile.toString());
        JobExecutionEntity jobExecution = batchClient.startJob("file-restart", properties);

        await().atMost(5, SECONDS)
                .until(() -> FAILED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        // header and first chunk of 3 committed, second chunk rolled back
        List<String> auctions = lines("auctions.csv");
        assertEquals(auctions.subList(0, 4), Files.readAllLines(outputFile));

        JobExecutionEntity restartExecution = batchClient.restartJobExecution(jobExecution.getExecutionId(), properties);

        await().atMost(5, SECONDS)
                .until(() -> batchClient.getJobExecution(restartExecution.getExecutionId()).getEndTime() != null);
        assertEquals(COMPLETED, batchClient.getJobExecution(restartExecution.getExecutionId()).getBatchStatus());

        // the header is not written again on restart
        assertEquals(auctions, Files.readAllLines(outputFile));
    }

    @Test
    void fileCommentsHeader(@TempDir Path tempDir) throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        Path outputFile = tempDir.resolve("auctions-output.csv");
        Properties properties = new Properties();
        properties.setProperty("outputFile", outputFile.toString());
        JobExecutionEntity jobExecution = batchClient.startJob("file-comments-header", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        // comments skipped when reading, header written back
        assertEquals(lines("auctions.csv"), Files.readAllLines(outputFile));
    }

    static List<String> lines(String resource) throws Exception {
        try (InputStream inputStream = FileItemTest.class.getClassLoader().getResourceAsStream(resource)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}
