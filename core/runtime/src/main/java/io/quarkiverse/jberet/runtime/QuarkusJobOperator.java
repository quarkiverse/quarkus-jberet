package io.quarkiverse.jberet.runtime;

import static org.jberet._private.BatchMessages.MESSAGES;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import jakarta.batch.operations.JobExecutionAlreadyCompleteException;
import jakarta.batch.operations.JobExecutionNotMostRecentException;
import jakarta.batch.operations.JobRestartException;
import jakarta.batch.operations.JobSecurityException;
import jakarta.batch.operations.JobStartException;
import jakarta.batch.operations.NoSuchJobException;
import jakarta.batch.operations.NoSuchJobExecutionException;
import jakarta.enterprise.inject.Vetoed;

import org.jberet.job.model.Job;
import org.jberet.operations.AbstractJobOperator;
import org.jberet.repository.ApplicationAndJobName;
import org.jberet.repository.JobRepository;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.spi.BatchEnvironment;

import io.quarkiverse.jberet.runtime.JBeretConfig.JobConfig;

@Vetoed
public class QuarkusJobOperator extends AbstractJobOperator {
    private final BatchEnvironment batchEnvironment;
    private final Map<String, Job> jobs;
    private final JBeretConfig config;

    public QuarkusJobOperator(
            final JBeretConfig config,
            final BatchEnvironment batchEnvironment,
            final Collection<Job> jobs) {
        this.batchEnvironment = batchEnvironment;
        this.jobs = jobs.stream().collect(Collectors.toMap(Job::getJobXmlName, job -> job));
        this.config = config;
    }

    @Override
    public long start(final String jobXMLName, final Properties jobParameters)
            throws JobStartException, JobSecurityException {
        return start(jobXMLName, jobParameters, null);
    }

    @Override
    public long start(String jobXMLName, Properties jobParameters, String user)
            throws JobStartException, JobSecurityException {

        // Add params for configuration. Don't override if already there.
        JobConfig jobConfig = config.job().get(jobXMLName);
        if (jobConfig != null && jobConfig.params() != null) {
            for (Map.Entry<String, String> param : jobConfig.params().entrySet()) {
                if (!jobParameters.containsKey(param.getKey())) {
                    jobParameters.setProperty(param.getKey(), param.getValue());
                }
            }
        }

        // for now, we assume that all job XML files were identified and parsed during build
        Job jobDefinition = jobs.get(jobXMLName);
        if (jobDefinition != null) {
            return super.start(jobDefinition, jobParameters, user);
        } else {
            throw new NoSuchJobException("Job with xml name " + jobXMLName + " was not found");
        }
    }

    @Override
    public long restart(final long executionId, final Properties restartParameters)
            throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException, JobExecutionNotMostRecentException,
            JobRestartException, JobSecurityException {
        return restart(executionId, restartParameters, null);
    }

    @Override
    public long restart(final long executionId, final Properties restartParameters, final String user)
            throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException, JobExecutionNotMostRecentException,
            JobRestartException, JobSecurityException {

        JobExecutionImpl originalToRestart = getJobExecutionImpl(executionId);
        JobInstanceImpl jobInstance = originalToRestart.getJobInstance();
        if (jobInstance == null) {
            throw MESSAGES.noSuchJobInstance(null);
        }

        String jobName = originalToRestart.getJobName();
        Job jobDefinition = jobInstance.getUnsubstitutedJob();
        if (jobDefinition == null) {
            ApplicationAndJobName applicationAndJobName = new ApplicationAndJobName(jobInstance.getApplicationName(), jobName);
            JobRepository repository = getJobRepository();
            jobDefinition = repository.getJob(applicationAndJobName);
            if (jobDefinition == null) {
                jobDefinition = jobs.get(jobName);
                repository.addJob(applicationAndJobName, jobDefinition);
            }
            jobInstance.setUnsubstitutedJob(jobDefinition);
        }

        if (jobDefinition != null) {
            return super.restart(executionId, restartParameters, user);
        } else {
            throw new NoSuchJobException("Job with xml name " + jobName + " was not found");
        }
    }

    @Override
    public BatchEnvironment getBatchEnvironment() {
        return batchEnvironment;
    }
}
