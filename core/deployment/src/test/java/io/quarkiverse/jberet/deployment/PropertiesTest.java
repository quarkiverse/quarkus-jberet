package io.quarkiverse.jberet.deployment;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.StepBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class PropertiesTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideRuntimeConfigKey("quarkus.transaction-manager.default-transaction-timeout", "60");

    @Test
    void run() {
        Properties jobParameters = new Properties();
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("job", jobParameters);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });

        assertEquals(1, Reader.properties.size());
        assertEquals("value", Reader.properties.get(0));
    }

    @ApplicationScoped
    public static class JobProducer {
        @Produces
        @Named
        public Job job() {
            return new JobBuilder("job")
                    .step(new StepBuilder("batchletStep").batchlet("batchlet").next("chunkStep").build())
                    .step(new StepBuilder("chunkStep").reader("reader").writer("writer")
                            .property("property", "value").build())
                    .build();
        }
    }

    @Named("batchlet")
    @Dependent
    public static class Batchlet extends AbstractBatchlet {
        @Override
        public String process() throws Exception {
            return BatchStatus.COMPLETED.name();
        }
    }

    @Named("reader")
    @Dependent
    public static class Reader extends AbstractItemReader {
        static List<String> properties = new ArrayList<>();

        @Inject
        StepContext stepContext;

        @Override
        public Object readItem() {
            String property = stepContext.getProperties().getProperty("property");
            if (property != null) {
                properties.add(property);
            }
            return null;
        }
    }

    @Named("writer")
    @Dependent
    public static class Writer extends AbstractItemWriter {
        @Inject
        StepContext stepContext;

        @Override
        public void writeItems(List<Object> items) {

        }
    }
}
