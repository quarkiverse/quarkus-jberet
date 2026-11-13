package io.quarkiverse.jberet.runtime.telemetry;

import org.jberet.spi.JobTask;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

/**
 * A {@link JobTask} that executes with the OpenTelemetry {@link Context} that was current when the task was
 * created. JBeret submits Jobs, Split Flows and Partitions as tasks executed by other threads, so this propagates the
 * {@link Context} of the submitting thread, and the spans of the task are children of the span that submitted it.
 * <p>
 * The {@link Context} must be captured on submission, and not when the task is handed to the executor, because
 * {@link org.jberet.spi.JobExecutor} may queue the task and execute it later from another thread.
 */
public class JobWithTelemetry implements JobTask {
    private final JobTask delegate;
    private final Context context;

    public JobWithTelemetry(final JobTask delegate) {
        this.delegate = delegate;
        this.context = Context.current();
    }

    @Override
    public int getRequiredRemainingPermits() {
        return delegate.getRequiredRemainingPermits();
    }

    @Override
    public void run() {
        try (Scope ignored = context.makeCurrent()) {
            delegate.run();
        }
    }
}
