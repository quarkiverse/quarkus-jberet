package io.quarkiverse.jberet.tck;

import static io.quarkiverse.jberet.tck.JobOperatorSetter.setJobOperator;

import javax.batch.operations.JobOperator;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ContextAndListenersTest extends com.ibm.jbatch.tck.tests.jslxml.ContextAndListenerTests {
    @Inject
    JobOperator jobOperator;

    @BeforeEach
    void beforeEach() throws Exception {
        setUp();
        setJobOperator(this, jobOperator);
    }

    @Test
    public void testExamineJobContextInArtifact() throws Exception {
        super.testExamineJobContextInArtifact();
    }

    @Test
    public void testExamineStepContextInArtifact() throws Exception {
        super.testExamineStepContextInArtifact();
    }

    @Test
    public void testOneArtifactIsJobAndStepListener() throws Exception {
        super.testOneArtifactIsJobAndStepListener();
    }

    @Test
    public void testgetException() throws Exception {
        super.testgetException();
    }

    @Test
    public void testgetExceptionListenerBased() throws Exception {
        super.testgetExceptionListenerBased();
    }

    @Test
    public void testJobContextIsUniqueForMainThreadAndPartitions() throws Exception {
        super.testJobContextIsUniqueForMainThreadAndPartitions();
    }

    @Test
    public void testJobContextIsUniqueForMainThreadAndFlowsInSplits() throws Exception {
        super.testJobContextIsUniqueForMainThreadAndFlowsInSplits();
    }

    @Test
    public void testStepContextIsUniqueForMainThreadAndPartitions() throws Exception {
        super.testStepContextIsUniqueForMainThreadAndPartitions();
    }
}
