package io.quarkiverse.jberet.it.scopes.stepscoped;

import jakarta.batch.runtime.BatchStatus;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.jberet.it.cdiscopes.AbstractQuarkusIT;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests for {@link org.jberet.cdi.StepScoped}.
 */
@QuarkusTest
class StepScopedIT extends AbstractQuarkusIT {
    static final String stepScopedTest = "stepScoped";
    static final String stepScopedFailedTest = "stepScopedFail";
    static final String stepScopedTest2 = "stepScoped2";
    static final String stepScopedPartitionedTest = "stepScopedPartitioned";

    @Test
    void stepScopedTest() throws Exception {
        final String stepName1Repeat3 = "stepScoped.step1TYPE stepScoped.step1TYPE stepScoped.step1TYPE stepScoped.step1METHOD stepScoped.step1METHOD stepScoped.step1METHOD stepScoped.step1FIELD stepScoped.step1FIELD stepScoped.step1FIELD";

        final String stepName2 = "stepScoped.step2TYPE stepScoped.step2METHOD stepScoped.step2FIELD";

        // same job, different steps, injected Foo should be different
        // same step, different artifact, injected Foo (into both batchlet and step
        // listener) should be the same

        startJobAndWait(stepScopedTest);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assertions.assertEquals(stepName1Repeat3, stepExecutions.get(0).getExitStatus());
        Assertions.assertEquals(stepName2, stepExecutions.get(1).getExitStatus());

        stepExecutions.clear();
        jobExecution = null;
        stepExecutions = null;

        final String job2StepName1 = "stepScoped2.step1TYPE stepScoped2.step1METHOD stepScoped2.step1FIELD";
        final String job2StepName2 = "stepScoped2.step2TYPE stepScoped2.step2METHOD stepScoped2.step2FIELD";

        // run a different job (stepScoped2) to check that a different Foo instance is
        // used within the scope of stepScoped2
        startJobAndWait(stepScopedTest2);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assertions.assertEquals(job2StepName1, stepExecutions.get(0).getExitStatus());
        Assertions.assertEquals(job2StepName2, stepExecutions.get(1).getExitStatus());
    }

    @Test
    void stepScopedPartitionedTest() throws Exception {
        startJobAndWait(stepScopedPartitionedTest);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        // step2 exit status is set in analyzer, to foo.stepNames.
        final String stepExitStatus = stepExecution0.getExitStatus();

        // beforeStep, afterStep, batchlet * 3 partitioins = 5
        // and there are 3 injections: Foo, FooMethodTarget & FooFieldTarget
        Assertions.assertEquals(
                "stepScopedPartitioned.step1TYPE stepScopedPartitioned.step1TYPE stepScopedPartitioned.step1TYPE stepScopedPartitioned.step1TYPE stepScopedPartitioned.step1TYPE stepScopedPartitioned.step1METHOD stepScopedPartitioned.step1METHOD stepScopedPartitioned.step1METHOD stepScopedPartitioned.step1METHOD stepScopedPartitioned.step1METHOD stepScopedPartitioned.step1FIELD stepScopedPartitioned.step1FIELD stepScopedPartitioned.step1FIELD stepScopedPartitioned.step1FIELD stepScopedPartitioned.step1FIELD",
                stepExitStatus);
    }

    @Test
    public void stepScopedFail() throws Exception {
        // injecting @StepScoped Foo into a job listener will fail
        startJobAndWait(stepScopedFailedTest);
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

}
