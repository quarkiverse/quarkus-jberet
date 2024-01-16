package io.quarkiverse.jberet.it.cdiscopes;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import jakarta.batch.operations.JobExecutionAlreadyCompleteException;
import jakarta.batch.operations.JobExecutionIsRunningException;
import jakarta.batch.operations.JobExecutionNotMostRecentException;
import jakarta.batch.operations.JobExecutionNotRunningException;
import jakarta.batch.operations.JobRestartException;
import jakarta.batch.operations.JobSecurityException;
import jakarta.batch.operations.JobStartException;
import jakarta.batch.operations.NoSuchJobException;
import jakarta.batch.operations.NoSuchJobExecutionException;
import jakarta.batch.operations.NoSuchJobInstanceException;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.StepExecution;

import org.jberet.job.model.Job;
import org.jberet.operations.JobOperatorImpl;
import org.jberet.repository.JobRepository;
import org.jberet.spi.BatchEnvironment;
import org.jberet.testapps.common.AbstractIT;

import io.quarkiverse.jberet.runtime.QuarkusJobOperator;

/**
 * Used in {@link AbstractQuarkusIT} to set {@link AbstractIT} jobOperator field
 * because it is declared as {@link JobOperatorImpl}
 */
public class JobOperatorImplQuarkusDelegate extends JobOperatorImpl {
    private final QuarkusJobOperator delegate;

    public JobOperatorImplQuarkusDelegate(QuarkusJobOperator delegate) {
        super(delegate.getBatchEnvironment());
        this.delegate = delegate;
    }

    /**
     * String .xml extension from XML job name
     *
     * @param jobXMLName
     * @return
     */
    private String jobXMLNameToJobName(String jobXMLName) {
        if (jobXMLName.endsWith(".xml")) {
            jobXMLName = jobXMLName.substring(0, jobXMLName.length() - 4);
        }
        return jobXMLName;
    }

    @Override
    public long start(String jobXMLName, Properties jobParameters) throws JobStartException, JobSecurityException {
        jobXMLName = jobXMLNameToJobName(jobXMLName);
        return delegate.start(jobXMLName, jobParameters);
    }

    @Override
    public long start(String jobXMLName, Properties jobParameters, String user)
            throws JobStartException, JobSecurityException {
        jobXMLName = jobXMLNameToJobName(jobXMLName);
        return delegate.start(jobXMLName, jobParameters, user);
    }

    @Override
    public JobRepository getJobRepository() {
        return delegate.getJobRepository();
    }

    @Override
    public long restart(long executionId, Properties restartParameters) throws JobExecutionAlreadyCompleteException,
            NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException, JobSecurityException {
        return delegate.restart(executionId, restartParameters);
    }

    @Override
    public long restart(long executionId, Properties restartParameters, String user)
            throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException,
            JobExecutionNotMostRecentException, JobRestartException, JobSecurityException {
        return delegate.restart(executionId, restartParameters, user);
    }

    @Override
    public long start(Job jobDefined, Properties jobParameters) throws JobStartException, JobSecurityException {
        return delegate.start(jobDefined, jobParameters);
    }

    @Override
    public BatchEnvironment getBatchEnvironment() {
        return delegate.getBatchEnvironment();
    }

    @Override
    public long start(Job jobDefined, Properties jobParameters, String user)
            throws JobStartException, JobSecurityException {
        return delegate.start(jobDefined, jobParameters, user);
    }

    @Override
    public void stop(long executionId)
            throws NoSuchJobExecutionException, JobExecutionNotRunningException, JobSecurityException {
        delegate.stop(executionId);
    }

    @Override
    public Set<String> getJobNames() throws JobSecurityException {
        return delegate.getJobNames();
    }

    @Override
    public int getJobInstanceCount(String jobName) throws NoSuchJobException, JobSecurityException {
        return delegate.getJobInstanceCount(jobName);
    }

    @Override
    public List<JobInstance> getJobInstances(String jobName, int start, int count)
            throws NoSuchJobException, JobSecurityException {
        return delegate.getJobInstances(jobName, start, count);
    }

    @Override
    public List<Long> getRunningExecutions(String jobName) throws NoSuchJobException, JobSecurityException {
        return delegate.getRunningExecutions(jobName);
    }

    @Override
    public Properties getParameters(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        return delegate.getParameters(executionId);
    }

    @Override
    public void abandon(long executionId)
            throws NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {
        delegate.abandon(executionId);
    }

    @Override
    public JobInstance getJobInstance(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        return delegate.getJobInstance(executionId);
    }

    @Override
    public List<JobExecution> getJobExecutions(JobInstance instance)
            throws NoSuchJobInstanceException, JobSecurityException {
        return delegate.getJobExecutions(instance);
    }

    @Override
    public List<Long> getJobExecutionsByJob(String jobName) {
        return delegate.getJobExecutionsByJob(jobName);
    }

    @Override
    public JobExecution getJobExecution(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        return delegate.getJobExecution(executionId);
    }

    @Override
    public List<StepExecution> getStepExecutions(long jobExecutionId)
            throws NoSuchJobExecutionException, JobSecurityException {
        return delegate.getStepExecutions(jobExecutionId);
    }
}
