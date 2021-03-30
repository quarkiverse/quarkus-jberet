package io.quarkiverse.jberet.it.scripting;

import static io.quarkus.test.common.http.TestHTTPResourceManager.getUri;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.batch.runtime.BatchStatus.COMPLETED;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ScriptingTest {
    @Test
    void groovyInline() throws Exception {
        BatchClient batchClient = new BatchClient(getUri());

        Properties properties = new Properties();
        properties.setProperty("testName", "groovyInline");
        JobExecutionEntity start = batchClient.startJob("batchletGroovyInline", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(start.getExecutionId()).getBatchStatus()));

        JobExecutionEntity end = batchClient.getJobExecution(start.getExecutionId());
        assertEquals("groovyInline", end.getExitStatus());
    }

    @Test
    void groovySrc() throws Exception {
        BatchClient batchClient = new BatchClient(getUri());

        Properties properties = new Properties();
        properties.setProperty("testName", "batchletGroovySrc");
        JobExecutionEntity start = batchClient.startJob("batchletGroovySrc", properties);

        await().atMost(5, SECONDS)
                .until(() -> COMPLETED.equals(batchClient.getJobExecution(start.getExecutionId()).getBatchStatus()));

        JobExecutionEntity end = batchClient.getJobExecution(start.getExecutionId());
        assertEquals("batchletGroovySrc", end.getExitStatus());
    }
}
