package io.quarkiverse.jberet.it.file;

import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static jakarta.batch.runtime.BatchStatus.FAILED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.HttpServer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@QuarkusTest
class JsonItemTest {
    static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
    static final TypeReference<List<Auction>> AUCTIONS = new TypeReference<>() {
    };

    HttpServer httpServer;

    @Test
    void jsonProperties(@TempDir Path tempDir) throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        Path outputFile = tempDir.resolve("auctions-output.json");
        Properties properties = new Properties();
        properties.setProperty("outputFile", outputFile.toString());
        JobExecutionEntity jobExecution = batchClient.startJob("json-properties", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        assertEquals(auctions(), OBJECT_MAPPER.readValue(outputFile.toFile(), AUCTIONS));
    }

    @Test
    void jsonBeans(@TempDir Path tempDir) throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        Path outputFile = tempDir.resolve("auctions-output.json");
        Properties properties = new Properties();
        properties.setProperty("outputFile", outputFile.toString());
        JobExecutionEntity jobExecution = batchClient.startJob("json-beans", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        assertEquals(auctions(), OBJECT_MAPPER.readValue(outputFile.toFile(), AUCTIONS));
    }

    @Test
    void jsonProgrammatic(@TempDir Path tempDir) throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        Path outputFile = tempDir.resolve("auctions-output.json");
        Properties properties = new Properties();
        properties.setProperty("outputFile", outputFile.toString());
        JobExecutionEntity jobExecution = batchClient.startJob("json-programmatic", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        assertEquals(auctions(), OBJECT_MAPPER.readValue(outputFile.toFile(), AUCTIONS));
    }

    @Test
    void jsonRestart(@TempDir Path tempDir) throws Exception {
        BatchClient batchClient = new BatchClient(httpServer.getLocalBaseUri().toString());
        Path outputFile = tempDir.resolve("auctions-output.json");
        Properties properties = new Properties();
        properties.setProperty("outputFile", outputFile.toString());
        JobExecutionEntity jobExecution = batchClient.startJob("json-restart", properties);

        await().atMost(5, SECONDS)
                .until(() -> FAILED.equals(batchClient.getJobExecution(jobExecution.getExecutionId()).getBatchStatus()));

        // first chunk of 3 committed, second chunk rolled back
        assertEquals(auctions().subList(0, 3), OBJECT_MAPPER.readValue(outputFile.toFile(), AUCTIONS));

        JobExecutionEntity restartExecution = batchClient.restartJobExecution(jobExecution.getExecutionId(), properties);

        await().atMost(5, SECONDS)
                .until(() -> batchClient.getJobExecution(restartExecution.getExecutionId()).getEndTime() != null);
        assertEquals(COMPLETED, batchClient.getJobExecution(restartExecution.getExecutionId()).getBatchStatus());

        assertEquals(auctions(), OBJECT_MAPPER.readValue(outputFile.toFile(), AUCTIONS));
    }

    static List<Auction> auctions() throws Exception {
        try (InputStream inputStream = JsonItemTest.class.getClassLoader().getResourceAsStream("auctions.json")) {
            return OBJECT_MAPPER.readValue(inputStream, AUCTIONS);
        }
    }
}
