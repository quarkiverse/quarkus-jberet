package io.quarkiverse.jberet.runtime;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.StepExecution;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import org.jberet.job.model.Job;
import org.jberet.repository.ApplicationAndJobName;
import org.jberet.repository.JobExecutionSelector;
import org.jberet.repository.JobRepository;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;

import io.quarkus.arc.Unremovable;

@Unremovable
@Singleton
@Transactional
public class QuarkusJobRepository implements JobRepository {
    private final JobRepository jobRepository;

    @Inject
    public QuarkusJobRepository(JBeretConfig config, Instance<JobRepositorySupplier> jobRepositories) {
        this.jobRepository = getJobRepository(config, jobRepositories);
    }

    private static JobRepository getJobRepository(JBeretConfig config, Instance<JobRepositorySupplier> jobRepositories) {
        for (JobRepositorySupplier jobRepository : jobRepositories) {
            if (config.repository().type().equals(jobRepository.getName())) {
                return jobRepository.get();
            }
        }
        throw new DeploymentException("No job repository found for " + config.repository().type());
    }

    @Override
    public void addJob(ApplicationAndJobName applicationAndJobName, Job job) {
        jobRepository.addJob(applicationAndJobName, job);
    }

    @Override
    public void removeJob(String jobId) {
        jobRepository.removeJob(jobId);
    }

    @Override
    public Job getJob(ApplicationAndJobName applicationAndJobName) {
        return jobRepository.getJob(applicationAndJobName);
    }

    @Override
    public Set<String> getJobNames() {
        return jobRepository.getJobNames();
    }

    @Override
    public boolean jobExists(String jobName) {
        return jobRepository.jobExists(jobName);
    }

    @Override
    public JobInstanceImpl createJobInstance(Job job, String applicationName, ClassLoader classLoader) {
        return jobRepository.createJobInstance(job, applicationName, classLoader);
    }

    @Override
    public void removeJobInstance(long jobInstanceId) {
        jobRepository.removeJobInstance(jobInstanceId);
    }

    @Override
    public JobInstance getJobInstance(long jobInstanceId) {
        return jobRepository.getJobInstance(jobInstanceId);
    }

    @Override
    public List<JobInstance> getJobInstances(String jobName) {
        return jobRepository.getJobInstances(jobName);
    }

    @Override
    public int getJobInstanceCount(String jobName) {
        return jobRepository.getJobInstanceCount(jobName);
    }

    @Override
    public JobExecutionImpl createJobExecution(JobInstanceImpl jobInstance, Properties jobParameters) {
        return jobRepository.createJobExecution(jobInstance, jobParameters);
    }

    @Override
    public JobExecution getJobExecution(long jobExecutionId) {
        return jobRepository.getJobExecution(jobExecutionId);
    }

    @Override
    public List<JobExecution> getJobExecutions(JobInstance jobInstance) {
        return jobRepository.getJobExecutions(jobInstance);
    }

    @Override
    public List<Long> getJobExecutionsByJob(String jobName) {
        return jobRepository.getJobExecutionsByJob(jobName);
    }

    @Override
    public List<Long> getJobExecutionsByJob(String jobName, Integer limit) {
        return jobRepository.getJobExecutionsByJob(jobName, limit);
    }

    @Override
    public void updateJobExecution(JobExecutionImpl jobExecution, boolean fullUpdate, boolean saveJobParameters) {
        jobRepository.updateJobExecution(jobExecution, fullUpdate, saveJobParameters);
    }

    @Override
    public void stopJobExecution(JobExecutionImpl jobExecution) {
        jobRepository.stopJobExecution(jobExecution);
    }

    @Override
    public List<Long> getRunningExecutions(String jobName) {
        return jobRepository.getRunningExecutions(jobName);
    }

    @Override
    public void removeJobExecutions(JobExecutionSelector jobExecutionSelector) {
        jobRepository.removeJobExecutions(jobExecutionSelector);
    }

    @Override
    public List<StepExecution> getStepExecutions(long jobExecutionId, ClassLoader classLoader) {
        return jobRepository.getStepExecutions(jobExecutionId, classLoader);
    }

    @Override
    public StepExecutionImpl createStepExecution(String stepName) {
        return jobRepository.createStepExecution(stepName);
    }

    @Override
    public void addStepExecution(JobExecutionImpl jobExecution, StepExecutionImpl stepExecution) {
        jobRepository.addStepExecution(jobExecution, stepExecution);
    }

    @Override
    public void updateStepExecution(StepExecution stepExecution) {
        jobRepository.updateStepExecution(stepExecution);
    }

    @Override
    public StepExecutionImpl findOriginalStepExecutionForRestart(String stepName, JobExecutionImpl jobExecutionToRestart,
            ClassLoader classLoader) {
        return jobRepository.findOriginalStepExecutionForRestart(stepName, jobExecutionToRestart, classLoader);
    }

    @Override
    public int countStepStartTimes(String stepName, long jobInstanceId) {
        return jobRepository.countStepStartTimes(stepName, jobInstanceId);
    }

    @Override
    public void addPartitionExecution(StepExecutionImpl enclosingStepExecution, PartitionExecutionImpl partitionExecution) {
        jobRepository.addPartitionExecution(enclosingStepExecution, partitionExecution);
    }

    @Override
    public List<PartitionExecutionImpl> getPartitionExecutions(long stepExecutionId, StepExecutionImpl stepExecution,
            boolean notCompletedOnly, ClassLoader classLoader) {
        return jobRepository.getPartitionExecutions(stepExecutionId, stepExecution, notCompletedOnly, classLoader);
    }

    @Override
    public void savePersistentData(JobExecution jobExecution, AbstractStepExecution stepOrPartitionExecution) {
        jobRepository.savePersistentData(jobExecution, stepOrPartitionExecution);
    }

    @Override
    public int savePersistentDataIfNotStopping(JobExecution jobExecution, AbstractStepExecution stepOrPartitionExecution) {
        return jobRepository.savePersistentDataIfNotStopping(jobExecution, stepOrPartitionExecution);
    }
}
