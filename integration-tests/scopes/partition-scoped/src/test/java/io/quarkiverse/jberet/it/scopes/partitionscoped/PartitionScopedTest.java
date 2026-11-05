package io.quarkiverse.jberet.it.scopes.partitionscoped;

import jakarta.batch.runtime.BatchStatus;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.jberet.it.cdiscopes.AbstractScopeTest;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests for {@link org.jberet.cdi.PartitionScoped}.
 */
@QuarkusTest
class PartitionScopedTest extends AbstractScopeTest {
    static final String partitionScopedTest = "partitionScopedPartitioned";
    static final String partitionScopedFailJobListenerTest = "partitionScopedFailJobListener";
    static final String partitionScopedFailStepListenerTest = "partitionScopedFailStepListener";

    @Test
    void partitionScopedTest() throws Exception {
        // same partition, different artifacts, injected Foo should be different
        // different partition, injected Foo should be the different

        startJobAndWait(partitionScopedTest);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        final String exitStatus = stepExecution0.getExitStatus();
        System.out.printf("step exit status: %s%n", exitStatus);
        // There's not guarantee which orders threads will be processed in, just check
        // the existStatus contains
        // each value from the expected data.
        for (String expected : PartitionScopePartitionAnalyzer.expectedData) {
            Assertions.assertTrue(exitStatus.contains(expected),
                    "Missing expected data '" + expected + "' in '" + exitStatus + "'");
        }
    }

    @Test
    void partitionScopedFailJobListener() throws Exception {
        // injecting @PartitionScoped Foo into a job listener will fail
        startJobAndWait(partitionScopedFailJobListenerTest);
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    @Test
    void partitionScopedFailStepListener() throws Exception {
        // injecting @PartitionScoped Foo into a step listener will fail
        startJobAndWait(partitionScopedFailStepListenerTest);
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }
}
