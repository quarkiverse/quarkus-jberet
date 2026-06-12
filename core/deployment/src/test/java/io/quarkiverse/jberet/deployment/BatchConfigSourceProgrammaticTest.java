package io.quarkiverse.jberet.deployment;

import static org.awaitility.Awaitility.await;

import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.StepBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BatchConfigSourceProgrammaticTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest();

    @Named("configProgrammaticBatchlet")
    @Dependent
    public static class ConfigProgrammaticBatchlet extends AbstractBatchlet {
        @Inject
        @ConfigProperty(name = "programmatic.name")
        Optional<String> name;

        @Override
        public String process() {
            if (name.isPresent() && "naruto".equals(name.get())) {
                return BatchStatus.COMPLETED.toString();
            } else {
                return BatchStatus.FAILED.toString();
            }
        }
    }

    @ApplicationScoped
    public static class JobProducer {
        @Produces
        @Named
        public Job programmaticConfigJob() {
            return new JobBuilder("programmaticConfigJob")
                    .step(new StepBuilder("step")
                            .batchlet("configProgrammaticBatchlet",
                                    new String[] { "programmatic.name", "#{jobParameters['programmatic.name']}" })
                            .build())
                    .build();
        }
    }

    @Test
    public void configPropertyFromProgrammaticJob() {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("programmatic.name", "naruto");
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("programmaticConfigJob", jobParameters);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });
    }
}
