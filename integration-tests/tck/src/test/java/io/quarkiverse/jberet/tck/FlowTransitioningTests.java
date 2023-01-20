package io.quarkiverse.jberet.tck;

import static io.quarkiverse.jberet.tck.JobOperatorSetter.setJobOperator;

import jakarta.batch.operations.JobOperator;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FlowTransitioningTests extends com.ibm.jbatch.tck.tests.jslxml.FlowTransitioningTests {
    @Inject
    JobOperator jobOperator;

    @BeforeEach
    void beforeEach() throws Exception {
        beforeTest();
        setJobOperator(this, jobOperator);
    }

    @Test
    public void testFlowTransitionToStep() throws Exception {
        super.testFlowTransitionToStep();
    }

    @Test
    public void testFlowTransitionToStepOutOfScope() throws Exception {
        super.testFlowTransitionToStepOutOfScope();
    }

    @Test
    public void testFlowTransitionToDecision() throws Exception {
        super.testFlowTransitionToDecision();
    }

    @Test
    public void testFlowTransitionWithinFlow() throws Exception {
        super.testFlowTransitionWithinFlow();
    }
}
