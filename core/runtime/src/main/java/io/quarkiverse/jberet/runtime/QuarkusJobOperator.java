package io.quarkiverse.jberet.runtime;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobException;
import javax.transaction.TransactionManager;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jberet.job.model.Job;
import org.jberet.operations.AbstractJobOperator;
import org.jberet.spi.BatchEnvironment;

import io.quarkiverse.jberet.runtime.JBeretConfig.JobConfig;

public class QuarkusJobOperator extends AbstractJobOperator {
    private final BatchEnvironment batchEnvironment;
    private final Map<String, Job> jobs;
    private final JBeretConfig config;

    public QuarkusJobOperator(
            final JBeretConfig config,
            final ManagedExecutor managedExecutor,
            final TransactionManager transactionManager,
            final List<Job> jobs) {

        this.batchEnvironment = new QuarkusBatchEnvironment(config, new QuarkusJobExecutor(managedExecutor),
                transactionManager);
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
        JobConfig jobConfig = config.job.get(jobXMLName);
        if (jobConfig != null && jobConfig.params != null) {
            for (Map.Entry<String, String> param : jobConfig.params.entrySet()) {
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
    public BatchEnvironment getBatchEnvironment() {
        return batchEnvironment;
    }
}
