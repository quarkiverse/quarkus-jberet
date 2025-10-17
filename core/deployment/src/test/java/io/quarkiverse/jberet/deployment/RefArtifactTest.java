package io.quarkiverse.jberet.deployment;

import static org.awaitility.Awaitility.await;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.Decider;
import jakarta.batch.api.chunk.AbstractCheckpointAlgorithm;
import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.batch.api.chunk.listener.AbstractChunkListener;
import jakarta.batch.api.chunk.listener.AbstractItemProcessListener;
import jakarta.batch.api.chunk.listener.AbstractItemReadListener;
import jakarta.batch.api.chunk.listener.AbstractItemWriteListener;
import jakarta.batch.api.chunk.listener.RetryProcessListener;
import jakarta.batch.api.chunk.listener.RetryReadListener;
import jakarta.batch.api.chunk.listener.RetryWriteListener;
import jakarta.batch.api.chunk.listener.SkipProcessListener;
import jakarta.batch.api.chunk.listener.SkipReadListener;
import jakarta.batch.api.chunk.listener.SkipWriteListener;
import jakarta.batch.api.listener.AbstractJobListener;
import jakarta.batch.api.listener.AbstractStepListener;
import jakarta.batch.api.partition.AbstractPartitionAnalyzer;
import jakarta.batch.api.partition.AbstractPartitionReducer;
import jakarta.batch.api.partition.PartitionCollector;
import jakarta.batch.api.partition.PartitionMapper;
import jakarta.batch.api.partition.PartitionPlan;
import jakarta.batch.api.partition.PartitionPlanImpl;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RefArtifactTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RefArtifactBatchlet.class)
                    .addAsManifestResource("ref-artifact.xml", "batch-jobs/ref-artifact.xml"));

    @Test
    void run() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("ref-artifact", new Properties());

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });
    }

    @Named
    public static class RefArtifactJobListener extends AbstractJobListener {
    }

    @Named
    public static class RefArtifactStepListener extends AbstractStepListener {
    }

    @Named
    public static class RefArtifactBatchlet extends AbstractBatchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }
    }

    @Named
    public static class RefArtifactItemReader extends AbstractItemReader {
        @Override
        public Object readItem() {
            return null;
        }
    }

    @Named
    public static class RefArtifactItemProcessor implements ItemProcessor {
        @Override
        public Object processItem(Object item) {
            return null;
        }
    }

    @Named
    public static class RefArtifactItemWriter extends AbstractItemWriter {
        @Override
        public void writeItems(List<Object> items) {
        }
    }

    @Named
    public static class RefArtifactCheckpoint extends AbstractCheckpointAlgorithm {
        @Override
        public boolean isReadyToCheckpoint() {
            return true;
        }
    }

    @Named
    public static class RefArtifactChunkListener extends AbstractChunkListener {
    }

    @Named
    public static class RefArtifactItemReadListener extends AbstractItemReadListener {
    }

    @Named
    public static class RefArtifactItemProcessListener extends AbstractItemProcessListener {
    }

    @Named
    public static class RefArtifactItemWriteListener extends AbstractItemWriteListener {
    }

    @Named
    public static class RefArtifactRetryReadListener implements RetryReadListener {
        @Override
        public void onRetryReadException(Exception ex) {
        }
    }

    @Named
    public static class RefArtifactRetryProcessListener implements RetryProcessListener {
        @Override
        public void onRetryProcessException(Object item, Exception ex) {
        }
    }

    @Named
    public static class RefArtifactRetryWriteListener implements RetryWriteListener {
        @Override
        public void onRetryWriteException(List<Object> items, Exception ex) {
        }
    }

    @Named
    public static class RefArtifactSkipReadListener implements SkipReadListener {
        @Override
        public void onSkipReadItem(Exception ex) {
        }
    }

    @Named
    public static class RefArtifactSkipProcessListener implements SkipProcessListener {
        @Override
        public void onSkipProcessItem(Object item, Exception ex) {
        }
    }

    @Named
    public static class RefArtifactSkipWriteListener implements SkipWriteListener {
        @Override
        public void onSkipWriteItem(List<Object> items, Exception ex) {
        }
    }

    @Named
    public static class RefArtifactCollector implements PartitionCollector {
        @Override
        public Serializable collectPartitionData() throws Exception {
            return new Serializable() {
            };
        }
    }

    @Named
    public static class RefArtifactAnalyzer extends AbstractPartitionAnalyzer {
    }

    @Named
    public static class RefArtifactReducer extends AbstractPartitionReducer {
    }

    @Named
    public static class RefArtifactMapper implements PartitionMapper {
        @Override
        public PartitionPlan mapPartitions() {
            return new PartitionPlanImpl();
        }
    }

    @Named
    public static class RefArtifactDecider implements Decider {
        @Override
        public String decide(StepExecution[] executions) {
            return "exit";
        }
    }
}
