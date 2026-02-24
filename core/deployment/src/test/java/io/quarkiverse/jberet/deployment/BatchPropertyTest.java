package io.quarkiverse.jberet.deployment;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.StepBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BatchPropertyTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest();

    @Test
    void batchProperty() {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("custom", "value");
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("job", jobParameters);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });
    }

    @ApplicationScoped
    public static class JobProducer {
        @Produces
        @Named
        public Job job() {
            return new JobBuilder("job")
                    .step(new StepBuilder("step")
                            .batchlet("batchPropertyBatchlet",
                                    new String[] { "int", "1" },
                                    new String[] { "custom", "value" },
                                    new String[] { "numbers", "one" },
                                    new String[] { "ctor", "two" })
                            .build())
                    .build();
        }
    }

    @Dependent
    @Named
    public static class BatchPropertyBatchlet extends AbstractBatchlet {
        @Inject
        @BatchProperty(name = "int")
        int primitiveInt;
        @Inject
        @BatchProperty(name = "custom")
        Custom custom;
        @Inject
        @BatchProperty
        Numbers numbers;

        public BatchPropertyBatchlet(@BatchProperty(name = "ctor") Numbers numbers) {
            assertEquals(Numbers.TWO, numbers);
        }

        @Override
        public String process() {
            assertEquals(1, primitiveInt);
            assertEquals("value", custom.value);
            assertEquals(Numbers.ONE, numbers);
            return BatchStatus.COMPLETED.toString();
        }
    }

    public static class Custom {
        String value;

        public Custom(String value) {
            this.value = value;
        }
    }

    public enum Numbers {
        ONE,
        TWO,
        THREE
    }
}
