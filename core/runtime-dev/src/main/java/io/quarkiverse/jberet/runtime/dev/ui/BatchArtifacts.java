package io.quarkiverse.jberet.runtime.dev.ui;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import jakarta.batch.api.partition.PartitionReducer;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.jberet.job.model.Chunk;
import org.jberet.job.model.Decision;
import org.jberet.job.model.Flow;
import org.jberet.job.model.InheritableJobElement;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.Partition;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Split;
import org.jberet.job.model.Step;
import org.jberet.spi.ArtifactFactory;

import io.quarkiverse.jberet.runtime.QuarkusJobOperator;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;

/**
 * Collects the batch artifacts referenced by the Jobs (with <code>ref</code>), how they resolve, and the named batch
 * artifacts available to use in Jobs.
 */
final class BatchArtifacts {
    static final String RESOLVED = "RESOLVED";
    static final String UNRESOLVED = "UNRESOLVED";
    static final String EXPRESSION = "EXPRESSION";

    private static final String BUILT_IN_PACKAGE = "io.quarkiverse.jberet.components.";
    private static final String DEV_PACKAGE = JBeretDevEvents.class.getPackageName() + ".";

    private static final Map<Class<?>, String> TYPES = new LinkedHashMap<>();
    static {
        TYPES.put(Batchlet.class, "Batchlet");
        TYPES.put(ItemReader.class, "ItemReader");
        TYPES.put(ItemProcessor.class, "ItemProcessor");
        TYPES.put(ItemWriter.class, "ItemWriter");
        TYPES.put(CheckpointAlgorithm.class, "CheckpointAlgorithm");
        TYPES.put(Decider.class, "Decider");
        TYPES.put(PartitionMapper.class, "PartitionMapper");
        TYPES.put(PartitionCollector.class, "PartitionCollector");
        TYPES.put(PartitionAnalyzer.class, "PartitionAnalyzer");
        TYPES.put(PartitionReducer.class, "PartitionReducer");
        TYPES.put(JobListener.class, "JobListener");
        TYPES.put(StepListener.class, "StepListener");
        TYPES.put(ChunkListener.class, "ChunkListener");
        TYPES.put(ItemReadListener.class, "ItemReadListener");
        TYPES.put(ItemProcessListener.class, "ItemProcessListener");
        TYPES.put(ItemWriteListener.class, "ItemWriteListener");
        TYPES.put(SkipReadListener.class, "SkipReadListener");
        TYPES.put(SkipProcessListener.class, "SkipProcessListener");
        TYPES.put(SkipWriteListener.class, "SkipWriteListener");
        TYPES.put(RetryReadListener.class, "RetryReadListener");
        TYPES.put(RetryProcessListener.class, "RetryProcessListener");
        TYPES.put(RetryWriteListener.class, "RetryWriteListener");
    }

    private final ArtifactFactory artifactFactory;
    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private final Map<String, List<ArtifactUsage>> usages = new TreeMap<>();

    private BatchArtifacts(final QuarkusJobOperator jobOperator) {
        this.artifactFactory = jobOperator.getBatchEnvironment().getArtifactFactory();
    }

    static List<ArtifactInfo> collect(final QuarkusJobOperator jobOperator) {
        return new BatchArtifacts(jobOperator).collect(jobOperator.getJobs());
    }

    private List<ArtifactInfo> collect(final Map<String, Job> jobs) {
        for (Map.Entry<String, Job> job : new TreeMap<>(jobs).entrySet()) {
            listeners(job.getKey(), job.getValue(), "Job listener");
            elements(job.getKey(), job.getValue().getJobElements());
        }

        // Artifacts used by the Jobs. The resolution is the same that JBeret does when running the Job
        Map<String, ArtifactInfo> artifacts = new TreeMap<>();
        for (Map.Entry<String, List<ArtifactUsage>> usage : usages.entrySet()) {
            String ref = usage.getKey();
            if (isExpression(ref)) {
                artifacts.put(ref, new ArtifactInfo(ref, null, List.of(), false, EXPRESSION, usage.getValue()));
                continue;
            }
            Class<?> artifactClass = resolve(ref);
            artifacts.put(ref, artifactClass != null
                    ? new ArtifactInfo(ref, artifactClass.getName(), types(List.of(artifactClass)),
                            artifactClass.getName().startsWith(BUILT_IN_PACKAGE), RESOLVED, usage.getValue())
                    : new ArtifactInfo(ref, null, List.of(), false, UNRESOLVED, usage.getValue()));
        }

        // Named batch artifacts not used by any Job
        BeanManager beanManager = Arc.container().beanManager();
        for (Bean<?> bean : beanManager.getBeans(Object.class, Any.Literal.INSTANCE)) {
            String name = bean.getName();
            if (name == null || artifacts.containsKey(name) || bean.getBeanClass().getName().startsWith(DEV_PACKAGE)) {
                continue;
            }
            List<String> types = types(bean.getTypes());
            if (!types.isEmpty()) {
                String className = bean instanceof InjectableBean<?> injectableBean
                        ? injectableBean.getImplementationClass().getName()
                        : bean.getBeanClass().getName();
                artifacts.put(name, new ArtifactInfo(name, className, types, className.startsWith(BUILT_IN_PACKAGE),
                        RESOLVED, List.of()));
            }
        }

        List<ArtifactInfo> result = new ArrayList<>(artifacts.values());
        // Used artifacts first
        result.sort(Comparator.comparing((ArtifactInfo artifact) -> artifact.usages().isEmpty())
                .thenComparing(ArtifactInfo::ref));
        return result;
    }

    private void elements(final String jobName, final List<JobElement> elements) {
        for (JobElement element : elements) {
            if (element instanceof Step step) {
                listeners(jobName, step, "Step listener");
                use(jobName, step.getId(), "Batchlet", step.getBatchlet());
                Chunk chunk = step.getChunk();
                if (chunk != null) {
                    use(jobName, step.getId(), "Reader", chunk.getReader());
                    use(jobName, step.getId(), "Processor", chunk.getProcessor());
                    use(jobName, step.getId(), "Writer", chunk.getWriter());
                    use(jobName, step.getId(), "Checkpoint algorithm", chunk.getCheckpointAlgorithm());
                }
                Partition partition = step.getPartition();
                if (partition != null) {
                    use(jobName, step.getId(), "Partition mapper", partition.getMapper());
                    use(jobName, step.getId(), "Partition collector", partition.getCollector());
                    use(jobName, step.getId(), "Partition analyzer", partition.getAnalyzer());
                    use(jobName, step.getId(), "Partition reducer", partition.getReducer());
                }
            } else if (element instanceof Decision decision) {
                use(jobName, decision.getId(), "Decider", decision.getRef());
            } else if (element instanceof Flow flow) {
                elements(jobName, flow.getJobElements());
            } else if (element instanceof Split split) {
                for (Flow flow : split.getFlows()) {
                    elements(jobName, flow.getJobElements());
                }
            }
        }
    }

    private void listeners(final String jobName, final InheritableJobElement element, final String role) {
        if (element.getListeners() != null) {
            String elementName = element instanceof Job ? null : element.getId();
            for (RefArtifact listener : element.getListeners().getListeners()) {
                // Skip the Dev UI listeners
                if (listener.getRef() != null && !listener.getRef().startsWith(DEV_PACKAGE)) {
                    use(jobName, elementName, role, listener);
                }
            }
        }
    }

    private void use(final String jobName, final String element, final String role, final RefArtifact artifact) {
        if (artifact != null) {
            use(jobName, element, role, artifact.getRef());
        }
    }

    private void use(final String jobName, final String element, final String role, final String ref) {
        // Artifacts defined with a script have no ref
        if (ref != null && !ref.isBlank()) {
            usages.computeIfAbsent(ref, k -> new ArrayList<>()).add(new ArtifactUsage(jobName, element, role));
        }
    }

    private Class<?> resolve(final String ref) {
        try {
            return artifactFactory.getArtifactClass(ref, classLoader);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isExpression(final String ref) {
        return ref.contains("#{");
    }

    private static List<String> types(final Iterable<? extends Type> types) {
        List<String> names = new ArrayList<>();
        for (Map.Entry<Class<?>, String> type : TYPES.entrySet()) {
            for (Type candidate : types) {
                Class<?> rawType = rawType(candidate);
                if (rawType != null && type.getKey().isAssignableFrom(rawType)) {
                    names.add(type.getValue());
                    break;
                }
            }
        }
        return names;
    }

    private static Class<?> rawType(final Type type) {
        if (type instanceof Class<?> cls) {
            return cls;
        } else if (type instanceof ParameterizedType parameterizedType
                && parameterizedType.getRawType() instanceof Class<?> cls) {
            return cls;
        }
        return null;
    }

    /**
     * A batch artifact.
     *
     * @param ref the name to reference the artifact in a Job
     * @param className the class that implements the artifact, if resolved
     * @param types the batch API types the artifact implements
     * @param builtIn if the artifact is provided by the JBeret components extension
     * @param status <code>RESOLVED</code>, <code>UNRESOLVED</code> (the Job fails when it uses the artifact), or
     *        <code>EXPRESSION</code> (the ref is an expression, resolved when the Job runs)
     * @param usages the Jobs and elements that use the artifact
     */
    public record ArtifactInfo(
            String ref,
            String className,
            List<String> types,
            boolean builtIn,
            String status,
            List<ArtifactUsage> usages) {
    }

    /**
     * @param jobName the Job name
     * @param element the id of the Job element (step or decision) that uses the artifact, or <code>null</code> for the
     *        Job
     * @param role how the artifact is used, for instance <code>Reader</code> or <code>Step listener</code>
     */
    public record ArtifactUsage(String jobName, String element, String role) {
    }
}
