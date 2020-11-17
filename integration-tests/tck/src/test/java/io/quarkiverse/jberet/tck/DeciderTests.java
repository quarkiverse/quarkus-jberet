package io.quarkiverse.jberet.tck;

import static io.quarkiverse.jberet.tck.JobOperatorSetter.setJobOperator;

import javax.batch.operations.JobOperator;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DeciderTests extends com.ibm.jbatch.tck.tests.jslxml.DeciderTests {
    @Inject
    JobOperator jobOperator;

    @BeforeEach
    void beforeEach() throws Exception {
        setUp();
        setJobOperator(this, jobOperator);
    }

    @Test
    public void testDeciderEndNormal() throws Exception {
        super.testDeciderEndNormal();
    }

    @Test
    public void testDeciderEndSpecial() throws Exception {
        super.testDeciderEndSpecial();
    }

    @Test
    public void testDeciderStopNormal() throws Exception {
        super.testDeciderStopNormal();
    }

    @Test
    public void testDeciderStopSpecial() throws Exception {
        super.testDeciderStopSpecial();
    }

    @Test
    public void testDeciderFailNormal() throws Exception {
        super.testDeciderFailNormal();
    }

    @Test
    public void testDeciderFailSpecial() throws Exception {
        super.testDeciderFailSpecial();
    }

    @Test
    public void testDeciderNextNormal() throws Exception {
        super.testDeciderNextNormal();
    }

    @Test
    public void testDeciderNextSpecial() throws Exception {
        super.testDeciderNextSpecial();
    }

    @Test
    public void testDeciderExitStatusIsSetOnJobContext() throws Exception {
        super.testDeciderExitStatusIsSetOnJobContext();
    }

    @Test
    public void testDeciderCannotbeFirstElementOnStart() throws Exception {
        super.testDeciderCannotbeFirstElementOnStart();
    }

    @Test
    public void testDeciderTransitionFromStepAndAllowRestart() throws Exception {
        super.testDeciderTransitionFromStepAndAllowRestart();
    }

    @Test
    public void testDeciderTransitionFromStepWithinFlowAndAllowRestart() throws Exception {
        super.testDeciderTransitionFromStepWithinFlowAndAllowRestart();
    }

    @Test
    public void testDeciderTransitionFromFlowAndAllowRestart() throws Exception {
        super.testDeciderTransitionFromFlowAndAllowRestart();
    }

    @Test
    public void testDeciderTransitionFromSplitAndAllowRestart() throws Exception {
        super.testDeciderTransitionFromSplitAndAllowRestart();
    }

    @Test
    public void testDeciderTransitionFromStepAndAllowRestartFalse() throws Exception {
        super.testDeciderTransitionFromStepAndAllowRestartFalse();
    }

    @Test
    public void testDeciderTransitionFromStepWithinFlowAndAllowRestartFalse() throws Exception {
        super.testDeciderTransitionFromStepWithinFlowAndAllowRestartFalse();
    }

    @Test
    public void testDeciderTransitionFromFlowAndAllowRestartFalse() throws Exception {
        super.testDeciderTransitionFromFlowAndAllowRestartFalse();
    }

    @Test
    public void testDeciderTransitionFromSplitAndAllowRestartFalse() throws Exception {
        super.testDeciderTransitionFromSplitAndAllowRestartFalse();
    }
}
