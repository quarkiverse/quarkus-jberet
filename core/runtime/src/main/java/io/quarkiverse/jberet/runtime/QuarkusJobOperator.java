package io.quarkiverse.jberet.runtime;

import static org.jberet._private.BatchMessages.MESSAGES;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.enterprise.inject.Vetoed;
import javax.transaction.TransactionManager;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jberet.job.model.Job;
import org.jberet.operations.AbstractJobOperator;
import org.jberet.repository.ApplicationAndJobName;
import org.jberet.repository.JobRepository;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.spi.BatchEnvironment;

import io.quarkiverse.jberet.runtime.JBeretConfig.JobConfig;
import io.quarkiverse.jberet.runtime.JBeretDataHolder.JBeretData;
import io.quarkus.runtime.ThreadPoolConfig;

@Vetoed
public class QuarkusJobOperator extends AbstractJobOperator {
    private final BatchEnvironment batchEnvironment;
    private final Map<String, Job> jobs;
    private final JBeretConfig config;

    public QuarkusJobOperator(
            final JBeretConfig config,
            final ThreadPoolConfig threadPoolConfig,
            final ManagedExecutor managedExecutor,
            final TransactionManager transactionManager,
            final JBeretData data) {

        QuarkusJobExecutor jobExecutor = new QuarkusJobExecutor(managedExecutor, threadPoolConfig, config);
        this.batchEnvironment = new QuarkusBatchEnvironment(config, jobExecutor, transactionManager, data);
        this.jobs = data.getJobs().stream().collect(Collectors.toMap(Job::getJobXmlName, job -> job));
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
