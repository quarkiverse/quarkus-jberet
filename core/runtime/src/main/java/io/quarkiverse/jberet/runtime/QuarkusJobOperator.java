package io.quarkiverse.jberet.runtime;

import static org.jberet._private.BatchMessages.MESSAGES;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

import io.quarkiverse.jberet.runtime.JBeretRuntimeConfig.JobConfig;

@Vetoed
public class QuarkusJobOperator extends AbstractJobOperator {
    private final BatchEnvironment batchEnvironment;
    private final Map<String, Job> jobs;
    private final JBeretRuntimeConfig config;

    public QuarkusJobOperator(
            final JBeretRuntimeConfig config,
            final BatchEnvironment batchEnvironment,
            final List<Job> jobs) {
        this.config = config;
        this.batchEnvironment = batchEnvironment;
        this.jobs = new HashMap<>(jobs.size());
        for (Job job : jobs) {
            this.jobs.put(job.getJobXmlName(), job);
            getJobRepository().addJob(new ApplicationAndJobName(batchEnvironment.getApplicationName(), job.getId()), job);
        }
    }

    @Override
    public long start(final String jobXMLName, final Properties jobParameters)
            throws JobStartException, JobSecurityException {
        return start(jobXMLName, jobParameters != null ? jobParameters : new Properties(), null);
    }

    @Override
    public long start(String jobXMLName, Properties jobParameters, String user)
            throws JobStartException, JobSecurityException {
        // for now, we assume that all job XML files were identified and parsed during build
        Job jobDefinition = jobs.get(jobXMLName);
        if (jobDefinition != null) {
            setJobParametersFromConfig(jobParameters, config.job().get(jobXMLName));
            return super.start(jobDefinition, jobParameters, user);
        } else {
            throw new NoSuchJobException("Job with xml name " + jobXMLName + " was not found");
        }
    }

    @Override
    public long restart(final long executionId, final Properties restartParameters)
            throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException, JobExecutionNotMostRecentException,
            JobRestartException, JobSecurityException {
        return restart(executionId, restartParameters != null ? restartParameters : new Properties(), null);
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

        // The execution and the repository use the Job id, the Job definitions and the configuration the Job XML name
        String jobName = originalToRestart.getJobName();
        String jobXmlName = getJobXmlName(originalToRestart);
        Job jobDefinition = jobInstance.getUnsubstitutedJob();
        if (jobDefinition == null) {
            ApplicationAndJobName applicationAndJobName = new ApplicationAndJobName(jobInstance.getApplicationName(), jobName);
            JobRepository repository = getJobRepository();
            jobDefinition = repository.getJob(applicationAndJobName);
            if (jobDefinition == null && jobXmlName != null) {
                jobDefinition = jobs.get(jobXmlName);
                repository.addJob(applicationAndJobName, jobDefinition);
            }
            jobInstance.setUnsubstitutedJob(jobDefinition);
        }

        if (jobDefinition != null) {
            if (jobXmlName != null) {
                setJobParametersFromConfig(restartParameters, config.job().get(jobXmlName));
            }
            return super.restart(executionId, restartParameters, user);
        } else {
            throw new NoSuchJobException("Job with id " + jobName + " was not found");
        }
    }

    /**
     * The Job XML name of an execution. JBeret stores it in the parameters of failed and stopped executions (the ones
     * that can be restarted). Otherwise, find the Job by its id.
     */
    private String getJobXmlName(final JobExecutionImpl execution) {
        Properties parameters = execution.getJobParameters();
        String jobXmlName = parameters != null ? parameters.getProperty(Job.JOB_XML_NAME) : null;
        if (jobXmlName != null && jobs.containsKey(jobXmlName)) {
            return jobXmlName;
        }
        for (Map.Entry<String, Job> job : jobs.entrySet()) {
            if (job.getValue().getId().equals(execution.getJobName())) {
                return job.getKey();
            }
        }
        return null;
    }

    /**
     * The Jobs available to this operator, keyed by the Job XML name (or the bean name, for Jobs produced with CDI).
     *
     * @return an unmodifiable {@link Map} of the available Jobs
     */
    public Map<String, Job> getJobs() {
        return Collections.unmodifiableMap(jobs);
    }

    @Override
    public BatchEnvironment getBatchEnvironment() {
        return batchEnvironment;
    }

    private static void setJobParametersFromConfig(final Properties jobParameters, final JobConfig jobConfig) {
        // Add params for configuration. Don't override if already there.
        for (Map.Entry<String, String> param : jobConfig.params().entrySet()) {
            jobParameters.putIfAbsent(param.getKey(), param.getValue());
        }
    }
}
