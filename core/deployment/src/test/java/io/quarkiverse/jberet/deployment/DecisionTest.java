package io.quarkiverse.jberet.deployment;

import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DecisionTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Decider.class, DecisionBatchletFirst.class, DecisionBatchletSecond.class,
                            DecisionBatchletThird.class)
                    .addAsManifestResource("decision.xml", "batch-jobs/decision.xml"));

    @Test
    void decision() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("decision", new Properties());

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });

        List<String> executedSteps = jobOperator.getStepExecutions(executionId).stream().map(StepExecution::getStepName)
                .collect(toList());

        assertEquals(2, executedSteps.size());
        assertTrue(executedSteps.stream().anyMatch(step -> step.equals("decision-step-1")));
        assertTrue(executedSteps.stream().anyMatch(step -> step.equals("decision-step-3")));
        assertTrue(executedSteps.stream().noneMatch(step -> step.equals("decision-step-2")));
        assertEquals(BatchStatus.COMPLETED, jobOperator.getJobExecution(executionId).getBatchStatus());
    }

    @Named
    @Dependent
    public static class Decider implements jakarta.batch.api.Decider {
        @Override
        public String decide(final StepExecution[] executions) {
            return "proceed";
        }
    }

    @Named
    @Dependent
    public static class DecisionBatchletFirst extends AbstractBatchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }
    }

    @Named
    @Dependent
    public static class DecisionBatchletSecond extends AbstractBatchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }
    }

    @Named
    @Dependent
    public static class DecisionBatchletThird extends AbstractBatchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }
    }
}
