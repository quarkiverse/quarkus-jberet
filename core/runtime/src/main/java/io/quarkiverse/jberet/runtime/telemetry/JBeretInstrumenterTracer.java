package io.quarkiverse.jberet.runtime.telemetry;

import static jakarta.batch.runtime.BatchStatus.FAILED;

import java.util.List;
import java.util.function.Consumer;

import jakarta.batch.runtime.BatchStatus;
import jakarta.inject.Singleton;

import org.jberet.job.model.Listeners;
import org.jberet.job.model.RefArtifact;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.quarkiverse.jberet.runtime.JobProcessor;
import io.quarkiverse.jberet.runtime.JobProcessor.JobProcessorBuilder;

/**
 * Creates the OpenTelemetry spans of Job executions. The spans use the same names as the
 * <a href=
 * "https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/spring/spring-batch-3.0">OpenTelemetry
 * Java Agent Spring Batch instrumentation</a>:
 * <ul>
 * <li><code>BatchJob &lt;job&gt;</code> for the Job</li>
 * <li><code>BatchJob &lt;job&gt;.&lt;step&gt;</code> for each Step</li>
 * <li><code>BatchJob &lt;job&gt;.&lt;step&gt;.Chunk</code> for each Chunk</li>
 * </ul>
 * The spans are created by Job, Step and Chunk listeners, added to every Job when the application starts.
 */
@Singleton
public class JBeretInstrumenterTracer {
    static final String INSTRUMENTATION_NAME = "io.quarkiverse.jberet";

    private final Instrumenter<String, BatchStatus> instrumenter;

    public JBeretInstrumenterTracer(final OpenTelemetry openTelemetry) {
        this.instrumenter = Instrumenter.<String, BatchStatus> builder(openTelemetry, INSTRUMENTATION_NAME, name -> name)
                .setSpanStatusExtractor(new SpanStatusExtractor<String, BatchStatus>() {
                    @Override
                    public void extract(
                            final SpanStatusBuilder spanStatusBuilder,
                            final String name,
                            final BatchStatus batchStatus,
                            final Throwable error) {
                        if (error != null || batchStatus == FAILED) {
                            spanStatusBuilder.setStatus(StatusCode.ERROR);
                        }
                    }
                })
                .buildInstrumenter();
    }

    /**
     * Starts a span with the current {@link Context} as the parent, and makes it current. Batch listeners always
     * execute their before and after methods on the same thread, so the returned {@link Span} must be ended on the
     * same thread.
     *
     * @param name the span name
     * @return the started {@link Span}, or <code>null</code> if the span should not be started
     */
    Span start(final String name) {
        Context parentContext = Context.current();
        if (!instrumenter.shouldStart(parentContext, name)) {
            return null;
        }
        Context context = instrumenter.start(parentContext, name);
        return new Span(name, context, context.makeCurrent());
    }

    /**
     * Ends a span started with {@link #start(String)}.
     *
     * @param span the {@link Span} to end, may be <code>null</code>
     * @param batchStatus the {@link BatchStatus} of the execution, may be <code>null</code>
     * @param error the error of the execution, may be <code>null</code>
     */
    void end(final Span span, final BatchStatus batchStatus, final Throwable error) {
        if (span != null) {
            span.scope().close();
            instrumenter.end(span.context(), span.name(), batchStatus, error);
        }
    }

    record Span(String name, Context context, Scope scope) {
    }

    /**
     * Adds the tracing listeners to the Job and to every Step. The tracing listeners are added before any other
     * listener, so the spans include the execution of the other listeners.
     *
     * @return the {@link JobProcessor} that adds the listeners
     */
    public static JobProcessor listeners() {
        return new JobProcessorBuilder()
                .jobListenersConsumer(new Consumer<Listeners>() {
                    @Override
                    public void accept(final Listeners listeners) {
                        listeners.getListeners().add(0, new RefArtifact(JBeretTracingJobListener.NAME));
                    }
                })
                .stepListenersConsumer(new Consumer<Listeners>() {
                    @Override
                    public void accept(final Listeners listeners) {
                        // JBeret only creates Chunk listeners in Chunk Steps, so it is safe to add it to every Step
                        listeners.getListeners().addAll(0, List.of(
                                new RefArtifact(JBeretTracingStepListener.NAME),
                                new RefArtifact(JBeretTracingChunkListener.NAME)));
                    }
                })
                .build();
    }
}
