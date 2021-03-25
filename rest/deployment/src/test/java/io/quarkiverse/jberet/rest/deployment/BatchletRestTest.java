package io.quarkiverse.jberet.rest.deployment;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.runtime.BatchStatus;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BatchletRestTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyBatchlet.class)
                    .addAsResource(new StringAsset("%test.quarkus.http.port=8081\n"), "application.properties")
                    .addAsManifestResource("batchlet.xml", "batch-jobs/batchlet.xml"));

    @Named("batchlet")
    @Dependent
    public static class DummyBatchlet implements Batchlet {

        @Inject
        @BatchProperty(name = "name")
        String name;

        @Override
        public String process() {
            if (!name.equals("david")) {
                throw new RuntimeException("Unexpected value injected to 'name': " + name);
            }
            return BatchStatus.COMPLETED.toString();
        }

        @Override
        public void stop() {
        }
    }

    @Inject
    BatchClient batchClient;

    @Test
    void rest() throws Exception {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("name", "david");
        JobExecutionEntity jobExecutionEntity = batchClient.startJob("batchlet", jobParameters);

        assertNotNull(jobExecutionEntity);
        assertEquals("batchlet-job", jobExecutionEntity.getJobName());
        assertEquals(BatchStatus.STARTING, jobExecutionEntity.getBatchStatus());

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecutionEntity jobExecution = batchClient.getJobExecution(jobExecutionEntity.getExecutionId());
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });
    }
}
