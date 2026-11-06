package io.quarkiverse.jberet.runtime.dev.ui;

import static jakarta.batch.runtime.BatchStatus.FAILED;
import static jakarta.batch.runtime.BatchStatus.STOPPED;
import static jakarta.batch.runtime.BatchStatus.STOPPING;

import java.util.Map;
import java.util.TreeMap;

import jakarta.batch.api.listener.StepListener;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.Metric;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Publishes the start and end of Step executions to the Dev UI. Added to every Step in Dev mode.
 */
@Named(JBeretDevStepListener.NAME)
@Dependent
public class JBeretDevStepListener implements StepListener {
    static final String NAME = "io.quarkiverse.jberet.runtime.dev.ui.JBeretDevStepListener";

    @Inject
    JobContext jobContext;
    @Inject
    StepContext stepContext;
    @Inject
    JBeretDevEvents events;

    @Override
    public void beforeStep() {
        events.publish("STEP_STARTED", jobContext.getJobName(), jobContext.getExecutionId(), stepContext.getStepName(),
                stepContext.getBatchStatus().name(), null, null, null);
    }

    @Override
    public void afterStep() {
        // The Step may still be STARTED when it completes successfully, because JBeret sets the final status after the listeners
        BatchStatus status = stepContext.getBatchStatus();
        Exception exception = stepContext.getException();
        String batchStatus;
        if (exception != null || status == FAILED) {
            batchStatus = FAILED.name();
        } else if (status == STOPPING || status == STOPPED) {
            batchStatus = STOPPED.name();
        } else {
            batchStatus = BatchStatus.COMPLETED.name();
        }

        Map<String, Long> metrics = new TreeMap<>();
        if (stepContext.getMetrics() != null) {
            for (Metric metric : stepContext.getMetrics()) {
                metrics.put(metric.getType().name(), metric.getValue());
            }
        }

        events.publish("STEP_ENDED", jobContext.getJobName(), jobContext.getExecutionId(), stepContext.getStepName(),
                batchStatus, stepContext.getExitStatus() != null ? stepContext.getExitStatus() : batchStatus, metrics,
                exception != null ? exception.toString() : null);
    }
}
