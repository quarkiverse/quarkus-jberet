package io.quarkiverse.jberet.runtime.telemetry;

import jakarta.batch.api.listener.JobListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkiverse.jberet.runtime.telemetry.JBeretInstrumenterTracer.Span;

/**
 * Creates the <code>BatchJob &lt;job&gt;</code> span of a Job execution. Added to every Job.
 */
@Named(JBeretTracingJobListener.NAME)
@Dependent
public class JBeretTracingJobListener implements JobListener {
    static final String NAME = "io.quarkiverse.jberet.runtime.telemetry.JBeretTracingJobListener";

    @Inject
    JobContext jobContext;
    @Inject
    JBeretInstrumenterTracer tracing;

    private Span span;

    @Override
    public void beforeJob() {
        span = tracing.start(spanName(jobContext.getJobName()));
    }

    @Override
    public void afterJob() {
        tracing.end(span, jobContext.getBatchStatus(), null);
        span = null;
    }

    private static String spanName(final String jobName) {
        return "BatchJob " + jobName;
    }
}
