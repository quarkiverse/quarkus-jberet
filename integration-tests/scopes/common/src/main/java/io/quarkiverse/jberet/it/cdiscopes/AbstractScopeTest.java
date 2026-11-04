package io.quarkiverse.jberet.it.cdiscopes;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.batch.runtime.StepExecution;
import jakarta.inject.Inject;

import org.jberet.runtime.JobExecutionImpl;

import io.quarkiverse.jberet.runtime.QuarkusJobOperator;

/**
 * Base class for the CDI scopes tests. Starts a Job and waits for its termination, exposing the resulting
 * {@link JobExecutionImpl} and {@link StepExecution} for assertions.
 */
public abstract class AbstractScopeTest {
    private static final long JOB_TIMEOUT_SECONDS = 30;

    @Inject
    QuarkusJobOperator jobOperator;

    protected JobExecutionImpl jobExecution;
    protected List<StepExecution> stepExecutions;
    protected StepExecution stepExecution0;

    protected void startJobAndWait(final String jobName) throws InterruptedException {
        long executionId = jobOperator.start(jobName, new Properties());
        jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(executionId);
        jobExecution.awaitTermination(JOB_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        stepExecutions = jobOperator.getStepExecutions(executionId);
        stepExecution0 = stepExecutions.isEmpty() ? null : stepExecutions.get(0);
    }
}
