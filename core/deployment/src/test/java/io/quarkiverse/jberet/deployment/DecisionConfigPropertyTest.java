package io.quarkiverse.jberet.deployment;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.StepExecution;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class DecisionConfigPropertyTest {
    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ConfigDecider.class, DecisionConfigBatchlet.class)
                    .addAsManifestResource("decision-config.xml", "batch-jobs/decision-config.xml"));

    @Test
    void decisionConfigProperty() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("decision-config", new Properties());

        await().atMost(5, SECONDS).until(() -> {
            BatchStatus status = jobOperator.getJobExecution(executionId).getBatchStatus();
            return status == BatchStatus.COMPLETED || status == BatchStatus.FAILED;
        });

        assertEquals(BatchStatus.COMPLETED, jobOperator.getJobExecution(executionId).getBatchStatus());
        assertEquals(2, jobOperator.getStepExecutions(executionId).size());
    }

    @Named
    @Dependent
    public static class ConfigDecider implements jakarta.batch.api.Decider {
        @Inject
        @ConfigProperty(name = "decision.exit-status", defaultValue = "none")
        String exitStatus;

        @Override
        public String decide(final StepExecution[] executions) {
            return exitStatus;
        }
    }

    @Named
    @Dependent
    public static class DecisionConfigBatchlet extends AbstractBatchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }
    }
}
