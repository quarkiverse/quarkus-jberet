package io.quarkiverse.jberet.tck;

import static io.quarkiverse.jberet.tck.JobOperatorSetter.setJobOperator;

import jakarta.batch.operations.JobOperator;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BatchletRestartStateMachineTests extends com.ibm.jbatch.tck.tests.jslxml.BatchletRestartStateMachineTests {
    @Inject
    JobOperator jobOperator;

    @BeforeEach
    void beforeEach() throws Exception {
        setUp();
        setJobOperator(this, jobOperator);
    }

    @Test
    public void testTransitionElementOnAttrValuesWithRestartJobParamOverrides() throws Exception {
        super.testTransitionElementOnAttrValuesWithRestartJobParamOverrides();
    }

    @Test
    public void testAllowStartIfCompleteRestartExecution() throws Exception {
        super.testAllowStartIfCompleteRestartExecution();
    }
}
