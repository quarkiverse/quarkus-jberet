package io.quarkiverse.jberet.runtime;

import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jberet.runtime.JobExecutionImpl;

@ApplicationScoped
public class QuarkusJobLauncher {
    @Inject
    JobOperator jobOperator;

    /**
     * Starts or restarts a job synchronously. If the most recent execution of the job is
     * {@link BatchStatus#FAILED} or {@link BatchStatus#STOPPED}, the job is restarted from its
     * last checkpoint. Otherwise, a new job instance is created. The method blocks until the job
     * completes.
     *
     * @param jobName the name of the job XML definition
     * @param jobParameters the job parameters
     * @return the completed {@link JobExecution}
     */
    public JobExecution run(String jobName, Properties jobParameters) throws Exception {
        long executionId = startOrRestart(jobName, jobParameters);

        JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(executionId);
        jobExecution.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

        return jobExecution;
    }

    private long startOrRestart(String jobName, Properties jobParameters) {
        List<JobInstance> instances = jobOperator.getJobInstances(jobName, 0, 1);
        if (!instances.isEmpty()) {
            List<JobExecution> executions = jobOperator.getJobExecutions(instances.get(0));
            if (!executions.isEmpty()) {
                JobExecution lastExecution = executions.stream()
                        .max(Comparator.comparingLong(JobExecution::getExecutionId))
                        .get();
                BatchStatus status = lastExecution.getBatchStatus();
                if (status == BatchStatus.FAILED || status == BatchStatus.STOPPED) {
                    return jobOperator.restart(lastExecution.getExecutionId(), jobParameters);
                }
            }
        }

        return jobOperator.start(jobName, jobParameters);
    }
}
