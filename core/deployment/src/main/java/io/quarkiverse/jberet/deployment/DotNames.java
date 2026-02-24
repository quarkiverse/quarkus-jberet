package io.quarkiverse.jberet.deployment;

import java.util.Set;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.batch.api.Decider;
import jakarta.batch.api.chunk.CheckpointAlgorithm;
import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.batch.api.chunk.ItemReader;
import jakarta.batch.api.chunk.ItemWriter;
import jakarta.batch.api.chunk.listener.ChunkListener;
import jakarta.batch.api.chunk.listener.ItemProcessListener;
import jakarta.batch.api.chunk.listener.ItemReadListener;
import jakarta.batch.api.chunk.listener.ItemWriteListener;
import jakarta.batch.api.chunk.listener.RetryProcessListener;
import jakarta.batch.api.chunk.listener.RetryReadListener;
import jakarta.batch.api.chunk.listener.RetryWriteListener;
import jakarta.batch.api.chunk.listener.SkipProcessListener;
import jakarta.batch.api.chunk.listener.SkipReadListener;
import jakarta.batch.api.chunk.listener.SkipWriteListener;
import jakarta.batch.api.listener.JobListener;
import jakarta.batch.api.listener.StepListener;
import jakarta.batch.api.partition.PartitionAnalyzer;
import jakarta.batch.api.partition.PartitionCollector;
import jakarta.batch.api.partition.PartitionMapper;
import jakarta.batch.api.partition.PartitionPlan;
import jakarta.batch.api.partition.PartitionReducer;

import org.jberet.job.model.Job;
import org.jberet.repository.JobRepository;
import org.jboss.jandex.DotName;

public class DotNames {
    public static final DotName JOB = DotName.createSimple(Job.class);
    public static final DotName JOB_REPOSITORY = DotName.createSimple(JobRepository.class);

    public static final DotName JOB_LISTENER = DotName.createSimple(JobListener.class);
    public static final DotName STEP_LISTENER = DotName.createSimple(StepListener.class);

    public static final DotName BATCHLET = DotName.createSimple(Batchlet.class);
    public static final DotName DECIDER = DotName.createSimple(Decider.class);

    public static final DotName READER = DotName.createSimple(ItemReader.class);
    public static final DotName READER_TYPED = DotName.createSimple(io.quarkiverse.jberet.runtime.api.ItemReader.class);
    public static final DotName PROCESSOR = DotName.createSimple(ItemProcessor.class);
    public static final DotName PROCESSOR_TYPED = DotName.createSimple(io.quarkiverse.jberet.runtime.api.ItemProcessor.class);
    public static final DotName WRITER = DotName.createSimple(ItemWriter.class);
    public static final DotName WRITER_TYPED = DotName.createSimple(io.quarkiverse.jberet.runtime.api.ItemWriter.class);
    public static final DotName CHECKPOINT_ALGORITHM = DotName.createSimple(CheckpointAlgorithm.class);
    public static final DotName READ_LISTENER = DotName.createSimple(ItemReadListener.class);
    public static final DotName PROCESS_LISTENER = DotName.createSimple(ItemProcessListener.class);
    public static final DotName WRITE_LISTENER = DotName.createSimple(ItemWriteListener.class);
    public static final DotName CHUNK_LISTENER = DotName.createSimple(ChunkListener.class);
    public static final DotName RETRY_READ_LISTENER = DotName.createSimple(RetryReadListener.class);
    public static final DotName RETRY_PROCESS_LISTENER = DotName.createSimple(RetryProcessListener.class);
    public static final DotName RETRY_WRITE_LISTENER = DotName.createSimple(RetryWriteListener.class);
    public static final DotName SKIP_READ_LISTENER = DotName.createSimple(SkipReadListener.class);
    public static final DotName SKIP_PROCESS_LISTENER = DotName.createSimple(SkipProcessListener.class);
    public static final DotName SKIP_WRITE_LISTENER = DotName.createSimple(SkipWriteListener.class);

    public static final DotName PARTITION_MAPPER = DotName.createSimple(PartitionMapper.class);
    public static final DotName PARTITION_PLAN = DotName.createSimple(PartitionPlan.class);
    public static final DotName PARTITION_COLLECTOR = DotName.createSimple(PartitionCollector.class);
    public static final DotName PARTITION_ANALYZER = DotName.createSimple(PartitionAnalyzer.class);
    public static final DotName PARTITION_REDUCER = DotName.createSimple(PartitionReducer.class);

    public static final DotName BATCH_PROPERTY = DotName.createSimple(BatchProperty.class);

    public static final Set<DotName> JOB_ELEMENTS = Set.of(
            JOB_LISTENER, STEP_LISTENER,
            BATCHLET,
            DECIDER,
            READER, PROCESSOR, WRITER, CHECKPOINT_ALGORITHM,
            READER_TYPED, PROCESSOR_TYPED, WRITER_TYPED,
            READ_LISTENER, PROCESS_LISTENER, WRITE_LISTENER, CHUNK_LISTENER,
            RETRY_READ_LISTENER, RETRY_PROCESS_LISTENER, RETRY_WRITE_LISTENER,
            SKIP_READ_LISTENER, SKIP_PROCESS_LISTENER, SKIP_WRITE_LISTENER,
            PARTITION_MAPPER, PARTITION_PLAN, PARTITION_COLLECTOR, PARTITION_ANALYZER, PARTITION_REDUCER);
}
