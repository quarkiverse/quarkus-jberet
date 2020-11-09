package io.quarkiverse.jberet.tck;

import static io.quarkiverse.jberet.tck.JobOperatorSetter.setJobOperator;

import javax.batch.operations.JobOperator;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class JobOperatorTests extends com.ibm.jbatch.tck.tests.jslxml.JobOperatorTests {
    @Inject
    JobOperator jobOperator;

    @BeforeEach
    void beforeEach() throws Exception {
        setUp();
        setJobOperator(this, jobOperator);
    }

    @Test
    public void testJobOperatorStart() throws Exception {
        super.testJobOperatorStart();
    }

    @Test
    public void testJobOperatorRestart() throws Exception {
        super.testJobOperatorRestart();
    }

    @Test
    public void testJobOperatorRestartAlreadyCompleteException() throws Exception {
        super.testJobOperatorRestartAlreadyCompleteException();
    }

    @Test
    public void testJobOperatorAbandonJobDuringARestart() throws Exception {
        super.testJobOperatorAbandonJobDuringARestart();
    }

    @Test
    public void testJobOperatorRestartJobAlreadyAbandoned() throws Exception {
        super.testJobOperatorRestartJobAlreadyAbandoned();
    }

    @Test
    public void testInvokeJobWithUserStop() throws Exception {
        super.testInvokeJobWithUserStop();
    }

    @Test
    public void testJobOperatorGetStepExecutions() throws Exception {
        super.testJobOperatorGetStepExecutions();
    }

    @Test
    public void testJobOpGetJobNames() throws Exception {
        super.testJobOpGetJobNames();
    }

    @Test
    public void testAbandoned() throws Exception {
        super.testAbandoned();
    }

    @Test
    public void testJobOpgetJobInstanceCount() throws Exception {
        super.testJobOpgetJobInstanceCount();
    }

    @Test
    public void testJobOpgetJobInstanceCountException() throws Exception {
        super.testJobOpgetJobInstanceCountException();
    }

    @Test
    public void testJobOpgetJobInstances() throws Exception {
        super.testJobOpgetJobInstances();
    }

    @Test
    public void testJobOpgetJobInstancesException() throws Exception {
        super.testJobOpgetJobInstancesException();
    }

    @Test
    public void testJobOperatorGetParameters() throws Exception {
        super.testJobOperatorGetParameters();
    }

    @Test
    public void testJobOperatorGetJobInstances() throws Exception {
        super.testJobOperatorGetJobInstances();
    }

    @Test
    public void testJobOperatorGetRunningJobExecutions() throws Exception {
        super.testJobOperatorGetRunningJobExecutions();
    }

    @Test
    public void testJobOperatorGetRunningJobInstancesException() throws Exception {
        super.testJobOperatorGetRunningJobInstancesException();
    }

    @Test
    public void testJobOperatorGetJobExecution() throws Exception {
        super.testJobOperatorGetJobExecution();
    }

    @Test
    public void testJobOperatorGetJobExecutions() throws Exception {
        super.testJobOperatorGetJobExecutions();
    }
}
