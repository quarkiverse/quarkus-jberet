package io.quarkiverse.jberet.runtime.dev.ui;

import jakarta.batch.api.listener.JobListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Publishes the start and end of Job executions to the Dev UI. Added to every Job in Dev mode.
 */
@Named(JBeretDevJobListener.NAME)
@Dependent
public class JBeretDevJobListener implements JobListener {
    static final String NAME = "io.quarkiverse.jberet.runtime.dev.ui.JBeretDevJobListener";

    @Inject
    JobContext jobContext;
    @Inject
    JBeretDevEvents events;

    @Override
    public void beforeJob() {
        events.publish("JOB_STARTED", jobContext.getJobName(), jobContext.getExecutionId(), null,
                jobContext.getBatchStatus().name(), null, null, null);
    }

    @Override
    public void afterJob() {
        String batchStatus = jobContext.getBatchStatus().name();
        String exitStatus = jobContext.getExitStatus() != null ? jobContext.getExitStatus() : batchStatus;
        events.publish("JOB_ENDED", jobContext.getJobName(), jobContext.getExecutionId(), null, batchStatus, exitStatus,
                null, null);
    }
}
