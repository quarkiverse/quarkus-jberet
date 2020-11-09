package io.quarkiverse.jberet.tck;

import static io.quarkiverse.jberet.tck.JobOperatorSetter.setJobOperator;

import javax.batch.operations.JobOperator;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ExecutionTests extends com.ibm.jbatch.tck.tests.jslxml.ExecutionTests {
    @Inject
    JobOperator jobOperator;

    @BeforeEach
    void beforeEach() throws Exception {
        setUp();
        setJobOperator(this, jobOperator);
    }

    @Test
    public void testInvokeJobWithOneBatchletStep() throws Exception {
        super.testInvokeJobWithOneBatchletStep();
    }

    @Test
    public void testInvokeJobWithTwoStepSequenceOfBatchlets() throws Exception {
        super.testInvokeJobWithTwoStepSequenceOfBatchlets();
    }

    @Test
    public void testInvokeJobWithFourStepSequenceOfBatchlets() throws Exception {
        super.testInvokeJobWithFourStepSequenceOfBatchlets();
    }

    @Test
    public void testInvokeJobWithNextElement() throws Exception {
        super.testInvokeJobWithNextElement();
    }

    @Test
    public void testInvokeJobWithFailElement() throws Exception {
        super.testInvokeJobWithFailElement();
    }

    @Test
    public void testInvokeJobWithStopElement() throws Exception {
        super.testInvokeJobWithStopElement();
    }

    @Test
    public void testInvokeJobWithEndElement() throws Exception {
        super.testInvokeJobWithEndElement();
    }

    @Test
    public void testInvokeJobSimpleChunk() throws Exception {
        super.testInvokeJobSimpleChunk();
    }

    @Test
    public void testInvokeJobChunkWithFullAttributes() throws Exception {
        super.testInvokeJobChunkWithFullAttributes();
    }

    @Test
    public void testInvokeJobUsingTCCL() throws Exception {
        super.testInvokeJobUsingTCCL();
    }

    @Test
    public void testCheckpoint() throws Exception {
        super.testCheckpoint();
    }

    @Test
    public void testSimpleFlow() throws Exception {
        super.testSimpleFlow();
    }
}
