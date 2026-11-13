package io.quarkiverse.jberet.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jberet.job.model.Chunk;
import org.jberet.job.model.Decision;
import org.jberet.job.model.Flow;
import org.jberet.job.model.InheritableJobElement;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.Listeners;
import org.jberet.job.model.Partition;
import org.jberet.job.model.Properties;
import org.jberet.job.model.PropertiesHolder;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Split;
import org.jberet.job.model.Step;

public class JobProcessor {
    private final List<Consumer<Job>> jobConsumers;
    private final List<Consumer<Listeners>> jobListenersConsumers;
    private final List<Consumer<Step>> stepConsumers;
    private final List<Consumer<Listeners>> stepListenersConsumers;
    private final List<Consumer<Flow>> flowConsumers;
    private final List<Consumer<RefArtifact>> batchletConsumers;
    private final List<Consumer<RefArtifact>> readerConsumers;
    private final List<Consumer<RefArtifact>> processorConsumers;
    private final List<Consumer<RefArtifact>> writerConsumers;
    private final List<Consumer<RefArtifact>> checkPointAlgorithmConsumers;
    private final List<Consumer<RefArtifact>> mapperConsumers;
    private final List<Consumer<RefArtifact>> collectorConsumers;
    private final List<Consumer<RefArtifact>> analyzerConsumers;
    private final List<Consumer<RefArtifact>> reducerConsumers;
    private final List<Consumer<RefArtifact>> deciderConsumers;

    private JobProcessor(
            List<Consumer<Job>> jobConsumers,
            List<Consumer<Listeners>> jobListenersConsumers,
            List<Consumer<Step>> stepConsumers,
            List<Consumer<Listeners>> stepListenersConsumers,
            List<Consumer<Flow>> flowConsumers,
            List<Consumer<RefArtifact>> batchletConsumers,
            List<Consumer<RefArtifact>> readerConsumers,
            List<Consumer<RefArtifact>> processorConsumers,
            List<Consumer<RefArtifact>> writerConsumers,
            List<Consumer<RefArtifact>> checkPointAlgorithmConsumers,
            List<Consumer<RefArtifact>> mapperConsumers,
            List<Consumer<RefArtifact>> collectorConsumers,
            List<Consumer<RefArtifact>> analyzerConsumers,
            List<Consumer<RefArtifact>> reducerConsumers,
            List<Consumer<RefArtifact>> deciderConsumers) {

        this.jobConsumers = jobConsumers;
        this.jobListenersConsumers = jobListenersConsumers;
        this.stepConsumers = stepConsumers;
        this.stepListenersConsumers = stepListenersConsumers;
        this.flowConsumers = flowConsumers;
        this.batchletConsumers = batchletConsumers;
        this.readerConsumers = readerConsumers;
        this.processorConsumers = processorConsumers;
        this.writerConsumers = writerConsumers;
        this.checkPointAlgorithmConsumers = checkPointAlgorithmConsumers;
        this.mapperConsumers = mapperConsumers;
        this.collectorConsumers = collectorConsumers;
        this.analyzerConsumers = analyzerConsumers;
        this.reducerConsumers = reducerConsumers;
        this.deciderConsumers = deciderConsumers;
    }

    public void processJob(Job job) {
        jobConsumers.forEach(consumer -> consumer.accept(withProperties(job)));
        jobListenersConsumers.forEach(consumer -> consumer.accept(withListeners(job)));

        // Job#getInheritingJobElements are not processed, because the elements are also in the Job elements (or in the
        // elements of a Flow), and the inheritance was already resolved when the Job was loaded
        for (JobElement jobElement : job.getJobElements()) {
            processJob(jobElement);
        }
    }

    private void processJob(JobElement jobElement) {
        if (jobElement instanceof Step step) {
            stepConsumers.forEach(consumer -> consumer.accept(withProperties(step)));

            if (step.getBatchlet() != null) {
                batchletConsumers.forEach(consumer -> consumer.accept(withProperties(step.getBatchlet())));
            }

            stepListenersConsumers.forEach(consumer -> consumer.accept(withListeners(step)));

            if (step.getChunk() != null) {
                Chunk chunk = step.getChunk();
                if (chunk.getReader() != null) {
                    readerConsumers.forEach(consumer -> consumer.accept(withProperties(chunk.getReader())));
                }
                if (chunk.getProcessor() != null) {
                    processorConsumers.forEach(consumer -> consumer.accept(withProperties(chunk.getProcessor())));
                }
                if (chunk.getWriter() != null) {
                    writerConsumers.forEach(consumer -> consumer.accept(withProperties(chunk.getWriter())));
                }
                if (chunk.getCheckpointAlgorithm() != null) {
                    checkPointAlgorithmConsumers
                            .forEach(consumer -> consumer.accept(withProperties(chunk.getCheckpointAlgorithm())));
                }
            }

            if (step.getPartition() != null) {
                Partition partition = step.getPartition();
                if (partition.getMapper() != null) {
                    mapperConsumers.forEach(consumer -> consumer.accept(withProperties(partition.getMapper())));
                }
                if (partition.getCollector() != null) {
                    collectorConsumers.forEach(consumer -> consumer.accept(withProperties(partition.getCollector())));
                }
                if (partition.getAnalyzer() != null) {
                    analyzerConsumers.forEach(consumer -> consumer.accept(withProperties(partition.getAnalyzer())));
                }
                if (partition.getReducer() != null) {
                    reducerConsumers.forEach(consumer -> consumer.accept(withProperties(partition.getReducer())));
                }
            }
        }

        if (jobElement instanceof Flow flow) {
            flowConsumers.forEach(consumer -> consumer.accept(flow));
            for (JobElement flowElement : flow.getJobElements()) {
                processJob(flowElement);
            }
        }

        if (jobElement instanceof Split split) {
            for (Flow flow : split.getFlows()) {
                processJob(flow);
            }
        }

        if (jobElement instanceof Decision decision) {
            // The Decider is not a RefArtifact in the Job model, so use the Decision properties
            RefArtifact decider = new RefArtifact(decision.getRef());
            decider.setProperties(withProperties(decision).getProperties());
            deciderConsumers.forEach(consumer -> consumer.accept(decider));
        }
    }

    /**
     * The {@link Listeners} of a Job or Step, so the listeners consumers can add listeners. Creates the
     * {@link Listeners} if the element doesn't have any, and copies the listeners, in case the list is not modifiable
     * (the recorded list).
     */
    private static Listeners withListeners(final InheritableJobElement element) {
        if (element.getListeners() == null) {
            element.setListeners(new Listeners());
        }
        element.getListeners().setListeners(new ArrayList<>(element.getListeners().getListeners()));
        element.getListeners().getListeners().forEach(JobProcessor::withProperties);
        return element.getListeners();
    }

    /**
     * Creates the {@link Properties} of a Job element or a batch artifact if it doesn't have any, so the consumers
     * can always read and add properties.
     */
    private static <T extends PropertiesHolder> T withProperties(final T holder) {
        if (holder.getProperties() == null) {
            holder.setProperties(new Properties());
        }
        return holder;
    }

    public static class JobProcessorBuilder {
        List<Consumer<Job>> jobConsumers = new ArrayList<>();
        List<Consumer<Listeners>> jobListenersConsumers = new ArrayList<>();
        List<Consumer<Step>> stepConsumers = new ArrayList<>();
        List<Consumer<Listeners>> stepListenersConsumers = new ArrayList<>();
        List<Consumer<Flow>> flowConsumers = new ArrayList<>();
        List<Consumer<RefArtifact>> batchletConsumers = new ArrayList<>();
        List<Consumer<RefArtifact>> readerConsumers = new ArrayList<>();
        List<Consumer<RefArtifact>> processorConsumers = new ArrayList<>();
        List<Consumer<RefArtifact>> writerConsumers = new ArrayList<>();
        List<Consumer<RefArtifact>> checkPointAlgorithmConsumers = new ArrayList<>();
        List<Consumer<RefArtifact>> mapperConsumers = new ArrayList<>();
        List<Consumer<RefArtifact>> collectorConsumers = new ArrayList<>();
        List<Consumer<RefArtifact>> analyzerConsumers = new ArrayList<>();
        List<Consumer<RefArtifact>> reducerConsumers = new ArrayList<>();
        List<Consumer<RefArtifact>> deciderConsumers = new ArrayList<>();

        public JobProcessorBuilder jobConsumer(Consumer<Job> consumer) {
            jobConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder jobListenersConsumer(final Consumer<Listeners> consumer) {
            jobListenersConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder stepConsumer(final Consumer<Step> consumer) {
            stepConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder stepListenersConsumer(final Consumer<Listeners> consumer) {
            stepListenersConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder flowConsumer(final Consumer<Flow> consumer) {
            flowConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder batchletConsumer(final Consumer<RefArtifact> consumer) {
            batchletConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder readerConsumer(final Consumer<RefArtifact> consumer) {
            readerConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder processorConsumer(final Consumer<RefArtifact> consumer) {
            processorConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder writerConsumer(final Consumer<RefArtifact> consumer) {
            writerConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder checkPointAlgorithmConsumer(final Consumer<RefArtifact> consumer) {
            checkPointAlgorithmConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder mapperConsumer(final Consumer<RefArtifact> consumer) {
            mapperConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder collectorConsumer(final Consumer<RefArtifact> consumer) {
            collectorConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder analyzerConsumer(final Consumer<RefArtifact> consumer) {
            analyzerConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder reducerConsumer(final Consumer<RefArtifact> consumer) {
            reducerConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder deciderConsumer(final Consumer<RefArtifact> consumer) {
            deciderConsumers.add(consumer);
            return this;
        }

        public JobProcessorBuilder inheritableJobElementConsumer(final Consumer<InheritableJobElement> consumer) {
            jobConsumers.add(consumer::accept);
            stepConsumers.add(consumer::accept);
            flowConsumers.add(consumer::accept);
            return this;
        }

        public JobProcessorBuilder refArtifactConsumer(final Consumer<RefArtifact> refArtifactConsumer) {
            jobListenersConsumers.add(listeners -> listeners.getListeners().forEach(refArtifactConsumer));
            stepListenersConsumers.add(listeners -> listeners.getListeners().forEach(refArtifactConsumer));
            batchletConsumers.add(refArtifactConsumer);
            readerConsumers.add(refArtifactConsumer);
            processorConsumers.add(refArtifactConsumer);
            writerConsumers.add(refArtifactConsumer);
            checkPointAlgorithmConsumers.add(refArtifactConsumer);
            mapperConsumers.add(refArtifactConsumer);
            collectorConsumers.add(refArtifactConsumer);
            analyzerConsumers.add(refArtifactConsumer);
            reducerConsumers.add(refArtifactConsumer);
            deciderConsumers.add(refArtifactConsumer);
            return this;
        }

        public JobProcessorBuilder propertiesHolderConsumer(final Consumer<PropertiesHolder> consumer) {
            jobConsumers.add(consumer::accept);
            stepConsumers.add(consumer::accept);
            refArtifactConsumer(consumer::accept);
            return this;
        }

        public JobProcessor build() {
            return new JobProcessor(
                    jobConsumers,
                    jobListenersConsumers,
                    stepConsumers,
                    stepListenersConsumers,
                    flowConsumers,
                    batchletConsumers,
                    readerConsumers,
                    processorConsumers,
                    writerConsumers,
                    checkPointAlgorithmConsumers,
                    mapperConsumers,
                    collectorConsumers,
                    analyzerConsumers,
                    reducerConsumers,
                    deciderConsumers);
        }
    }
}
