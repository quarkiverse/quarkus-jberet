package io.quarkiverse.jberet.runtime.telemetry;

import jakarta.batch.api.listener.StepListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkiverse.jberet.runtime.telemetry.JBeretInstrumenterTracer.Span;

/**
 * Creates the <code>BatchJob &lt;job&gt;.&lt;step&gt;</code> span of a Step execution. Added to every Step.
 */
@Named(JBeretTracingStepListener.NAME)
@Dependent
public class JBeretTracingStepListener implements StepListener {
    static final String NAME = "io.quarkiverse.jberet.runtime.telemetry.JBeretTracingStepListener";

    @Inject
    JobContext jobContext;
    @Inject
    StepContext stepContext;
    @Inject
    JBeretInstrumenterTracer tracing;

    private Span span;

    @Override
    public void beforeStep() {
        span = tracing.start(spanName(jobContext.getJobName(), stepContext.getStepName()));
    }

    @Override
    public void afterStep() {
        // The Step may still be STARTED, because JBeret sets the final status after the listeners
        tracing.end(span, stepContext.getBatchStatus(), stepContext.getException());
        span = null;
    }

    private static String spanName(final String jobName, final String stepName) {
        return "BatchJob " + jobName + "." + stepName;
    }
}
