package io.quarkiverse.jberet.runtime.telemetry;

import jakarta.batch.api.chunk.listener.ChunkListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkiverse.jberet.runtime.telemetry.JBeretInstrumenterTracer.Span;

/**
 * Creates the <code>BatchJob &lt;job&gt;.&lt;step&gt;.Chunk</code> span of each Chunk. Added to every Chunk Step.
 */
@Named(JBeretTracingChunkListener.NAME)
@Dependent
public class JBeretTracingChunkListener implements ChunkListener {
    static final String NAME = "io.quarkiverse.jberet.runtime.telemetry.JBeretTracingChunkListener";

    @Inject
    JobContext jobContext;
    @Inject
    StepContext stepContext;
    @Inject
    JBeretInstrumenterTracer tracing;

    private Span span;

    @Override
    public void beforeChunk() {
        span = tracing.start(spanName(jobContext.getJobName(), stepContext.getStepName()));
    }

    @Override
    public void onError(final Exception ex) {
        // JBeret may call onError more than once for the same failure, or after afterChunk
        tracing.end(span, null, ex);
        span = null;
    }

    @Override
    public void afterChunk() {
        tracing.end(span, null, null);
        span = null;
    }

    private static String spanName(final String jobName, final String stepName) {
        return "BatchJob " + jobName + "." + stepName + ".Chunk";
    }
}
