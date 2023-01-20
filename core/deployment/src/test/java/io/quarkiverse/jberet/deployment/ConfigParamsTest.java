package io.quarkiverse.jberet.deployment;

import static org.awaitility.Awaitility.await;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigParamsTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ConfigBatchlet.class)
                    .addAsResource(new StringAsset("quarkus.jberet.job.\"batchlet\".params.name=naruto"),
                            "application.properties")
                    .addAsManifestResource("batchlet.xml", "batch-jobs/batchlet.xml"));

    @Named("batchlet")
    @Dependent
    public static class ConfigBatchlet extends AbstractBatchlet {
        @Inject
        @ConfigProperty(name = "name", defaultValue = "guest")
        String name;

        @Override
        public String process() {
            if ("naruto".equals(name)) {
                return BatchStatus.COMPLETED.toString();
            } else {
                return BatchStatus.FAILED.toString();
            }
        }
    }

    @Test
    public void configBatchlet() {
        Properties jobParameters = new Properties();
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("batchlet", jobParameters);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });
    }
}
