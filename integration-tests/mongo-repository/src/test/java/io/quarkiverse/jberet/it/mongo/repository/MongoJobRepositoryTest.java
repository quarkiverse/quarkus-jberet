package io.quarkiverse.jberet.it.mongo.repository;

import static jakarta.batch.runtime.BatchStatus.COMPLETED;
import static jakarta.batch.runtime.BatchStatus.FAILED;
import static jakarta.batch.runtime.BatchStatus.STARTED;
import static jakarta.batch.runtime.BatchStatus.STOPPING;
import static jakarta.batch.runtime.Metric.MetricType.READ_COUNT;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Properties;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.Metric;
import jakarta.batch.runtime.StepExecution;
import jakarta.inject.Inject;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.StepBuilder;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoClient;

import io.quarkiverse.jberet.components.runtime.repository.MongoJobRepository;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Reads what the application repository stored using a new {@link MongoJobRepository}. A new instance has an empty
 * cache, like a repository in a restarted application or in another node, so everything comes from the database.
 */
@QuarkusTest
class MongoJobRepositoryTest {
    @Inject
    MongoClient mongoClient;
    @Inject
    JobOperator jobOperator;

    @Test
    void restartFromPersistedCheckpoint() {
        Properties properties = new Properties();
        properties.setProperty("mongo.reader.fail.at", "3");
        long executionId = jobOperator.start("mongo-chunk", properties);
        awaitStatus(executionId, FAILED);

        ClassLoader classLoader = getClass().getClassLoader();
        MongoJobRepository failed = repository();
        JobExecutionImpl failedExecution = failed.getJobExecution(executionId);
        assertEquals(FAILED, failedExecution.getBatchStatus());
        assertEquals("3", failedExecution.getJobParameters().getProperty("mongo.reader.fail.at"));
        assertNull(failed.getJobExecution(Long.MAX_VALUE));

        List<StepExecution> failedSteps = failed.getStepExecutions(executionId, classLoader);
        assertEquals(1, failedSteps.size());
        assertEquals(FAILED, failedSteps.get(0).getBatchStatus());

        StepExecutionImpl original = failed.findOriginalStepExecutionForRestart("chunkStep", failedExecution, classLoader);
        assertEquals(2, original.getReaderCheckpointInfo());
        assertNull(failed.findOriginalStepExecutionForRestart("unknown", failedExecution, classLoader));

        properties.setProperty("mongo.reader.fail.at", "-1");
        long restartId = jobOperator.restart(executionId, properties);
        awaitStatus(restartId, COMPLETED);

        MongoJobRepository completed = repository();
        StepExecution restartedStep = completed.getStepExecutions(restartId, classLoader).get(0);
        assertEquals(COMPLETED, restartedStep.getBatchStatus());
        assertEquals(4, metric(restartedStep, READ_COUNT));

        long instanceId = completed.getJobExecution(restartId).getJobInstance().getInstanceId();
        assertEquals(2, completed.countStepStartTimes("chunkStep", instanceId));
        assertEquals(0, completed.countStepStartTimes("unknown", instanceId));
        assertEquals(2, completed.getJobExecutions(completed.getJobInstance(instanceId)).size());

        List<Long> executions = completed.getJobExecutionsByJob("mongo-chunk");
        assertTrue(executions.indexOf(restartId) < executions.indexOf(executionId));
        assertEquals(1, completed.getJobExecutionsByJob("mongo-chunk", 1).size());
        assertFalse(completed.getRunningExecutions("mongo-chunk").contains(restartId));
    }

    @Test
    void partitions() {
        long executionId = jobOperator.start("mongo-partition", new Properties());
        awaitStatus(executionId, COMPLETED);

        ClassLoader classLoader = getClass().getClassLoader();
        MongoJobRepository repository = repository();
        StepExecution step = repository.getStepExecutions(executionId, classLoader).get(0);

        List<PartitionExecutionImpl> partitions = repository.getPartitionExecutions(step.getStepExecutionId(),
                (StepExecutionImpl) step, false, classLoader);
        assertEquals(2, partitions.size());
        for (PartitionExecutionImpl partition : partitions) {
            assertEquals(COMPLETED, partition.getBatchStatus());
            assertEquals(CountingItemReader.ITEMS, partition.getReaderCheckpointInfo());
        }
        assertTrue(repository.getPartitionExecutions(step.getStepExecutionId(), (StepExecutionImpl) step, true, classLoader)
                .isEmpty());
    }

    @Test
    void stopFromAnotherNode() {
        Job job = new JobBuilder("mongo-stop-node").step(new StepBuilder("step")
                .batchlet("stoppableBatchlet").build()).build();

        MongoJobRepository repository = repository();
        JobInstanceImpl jobInstance = repository.createJobInstance(job, "app", getClass().getClassLoader());
        JobExecutionImpl jobExecution = repository.createJobExecution(jobInstance, new Properties());
        jobExecution.setBatchStatus(STARTED);
        repository.updateJobExecution(jobExecution, false, false);

        StepExecutionImpl stepExecution = repository.createStepExecution("step");
        stepExecution.setBatchStatus(STARTED);
        repository.addStepExecution(jobExecution, stepExecution);

        PartitionExecutionImpl partitionExecution = new PartitionExecutionImpl(stepExecution);
        partitionExecution.setPartitionId(0);
        partitionExecution.setBatchStatus(STARTED);
        repository.addPartitionExecution(stepExecution, partitionExecution);

        assertEquals(1, repository.savePersistentDataIfNotStopping(jobExecution, stepExecution));
        assertEquals(1, repository.savePersistentDataIfNotStopping(jobExecution, partitionExecution));

        repository.stopJobExecution(jobExecution);

        assertEquals(0, repository.savePersistentDataIfNotStopping(jobExecution, stepExecution));
        assertEquals(0, repository.savePersistentDataIfNotStopping(jobExecution, partitionExecution));

        ClassLoader classLoader = getClass().getClassLoader();
        MongoJobRepository other = repository();
        assertEquals(STOPPING, other.getJobExecution(jobExecution.getExecutionId()).getBatchStatus());
        assertEquals(STOPPING,
                other.getStepExecutions(jobExecution.getExecutionId(), classLoader).get(0).getBatchStatus());
        assertEquals(STOPPING, other.getPartitionExecutions(stepExecution.getStepExecutionId(), null, false, classLoader)
                .get(0).getBatchStatus());
    }

    @Test
    void sequences() {
        Job job = new JobBuilder("mongo-sequence").step(new StepBuilder("step")
                .batchlet("stoppableBatchlet").build()).build();

        JobInstanceImpl first = repository().createJobInstance(job, "app", getClass().getClassLoader());
        JobInstanceImpl second = repository().createJobInstance(job, "app", getClass().getClassLoader());
        assertNotEquals(first.getInstanceId(), second.getInstanceId());
    }

    private MongoJobRepository repository() {
        return new MongoJobRepository(mongoClient, "batch");
    }

    private void awaitStatus(final long executionId, final BatchStatus batchStatus) {
        await().atMost(10, SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return batchStatus.equals(jobExecution.getBatchStatus());
        });
    }

    private static long metric(final StepExecution stepExecution, final Metric.MetricType metricType) {
        for (Metric metric : stepExecution.getMetrics()) {
            if (metric.getType() == metricType) {
                return metric.getValue();
            }
        }
        throw new IllegalArgumentException(metricType.name());
    }
}
