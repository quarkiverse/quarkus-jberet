package io.quarkiverse.jberet.deployment;

import static io.quarkiverse.jberet.runtime.JBeretRepositoryTypeUtil.normalize;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;
import static org.jboss.jandex.AnnotationValue.createStringValue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.batch.operations.BatchRuntimeException;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.inject.Named;

import org.jberet.cdi.JobScoped;
import org.jberet.cdi.PartitionScoped;
import org.jberet.cdi.StepScoped;
import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.creation.BatchBeanProducer;
import org.jberet.job.model.BatchArtifacts;
import org.jberet.job.model.Decision;
import org.jberet.job.model.Flow;
import org.jberet.job.model.InheritableJobElement;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Script;
import org.jberet.job.model.Split;
import org.jberet.job.model.Step;
import org.jberet.job.model.Transition;
import org.jberet.repository.JobRepository;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.tools.MetaInfBatchJobsJobXmlResolver;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import io.quarkiverse.jberet.runtime.JBeretConfig;
import io.quarkiverse.jberet.runtime.JBeretConfig.JobConfig;
import io.quarkiverse.jberet.runtime.JBeretConfigSourceFactoryBuilder;
import io.quarkiverse.jberet.runtime.JBeretInMemoryJobRepositoryProducer;
import io.quarkiverse.jberet.runtime.JBeretJdbcJobRepositoryProducer;
import io.quarkiverse.jberet.runtime.JBeretProducer;
import io.quarkiverse.jberet.runtime.JBeretRecorder;
import io.quarkiverse.jberet.runtime.JobsProducer;
import io.quarkiverse.jberet.runtime.QuarkusJobScheduler;
import io.quarkiverse.jberet.runtime.scope.QuarkusJobScopedContextImpl;
import io.quarkiverse.jberet.runtime.scope.QuarkusPartitionScopedContextImpl;
import io.quarkiverse.jberet.runtime.scope.QuarkusStepScopedContextImpl;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.ThreadPoolConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.util.GlobUtil;

public class JBeretProcessor {

    private static final Logger log = Logger.getLogger("io.quarkiverse.jberet");

    private static final DotName JOB = DotName.createSimple(Job.class);

    @BuildStep
    public void registerExtension(BuildProducer<FeatureBuildItem> feature, BuildProducer<CapabilityBuildItem> capability) {
        feature.produce(new FeatureBuildItem("jberet"));
    }

    /**
     * Prevent JobOperatorContext$DefaultHolder from eagerly initializing because it depends on a ServiceLoader
     * entry for the BatchRuntime, which we don't use. With this trigger turned off, it won't ever be initialized.
     */
    @BuildStep
    public RuntimeInitializedClassBuildItem runtimeInitializedDefaultHolder() {
        return new RuntimeInitializedClassBuildItem("org.jberet.spi.JobOperatorContext$DefaultHolder");
    }

    @BuildStep
    public void batchScopes(ContextRegistrationPhaseBuildItem c, BuildProducer<ContextConfiguratorBuildItem> v) {
        v.produce(new ContextConfiguratorBuildItem(
                c.getContext().configure(JobScoped.class).contextClass(QuarkusJobScopedContextImpl.class),
                c.getContext().configure(StepScoped.class).contextClass(QuarkusStepScopedContextImpl.class),
                c.getContext().configure(PartitionScoped.class).contextClass(QuarkusPartitionScopedContextImpl.class)));
    }

    @BuildStep
    public void config(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(JBeretConfigSourceFactoryBuilder.class.getName()));
    }

    @BuildStep
    public void additionalBeans(
            JBeretConfig config,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer,
            BuildProducer<BatchArtifactBuildItem> batchArtifact) throws Exception {

        additionalBeans.produce(new AdditionalBeanBuildItem(BatchBeanProducer.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(JBeretProducer.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(JobsProducer.class));

        switch (normalize(config.repository().type())) {
            case JBeretInMemoryJobRepositoryProducer.TYPE:
                additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(JBeretInMemoryJobRepositoryProducer.class));
                break;
            case JBeretJdbcJobRepositoryProducer.TYPE:
                additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(JBeretJdbcJobRepositoryProducer.class));
                break;
        }

        Map<String, String> batchArtifacts = getBatchArtifacts();
        for (String artifact : batchArtifacts.keySet()) {
            additionalBeans.produce(new AdditionalBeanBuildItem(artifact));
            ClassInfo classInfo = combinedIndex.getIndex().getClassByName(artifact);
            if (classInfo != null) {
                String named = batchArtifacts.get(artifact);
                String alias = named;
                AnnotationInstance namedAnnotation = classInfo.annotation(DotNames.NAMED);
                if (namedAnnotation != null && namedAnnotation.value() != null) {
                    alias = namedAnnotation.value().asString();
                }
                batchArtifact.produce(new BatchArtifactBuildItem(artifact, named, alias));
            }
        }

        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(final AnnotationTarget.Kind kind) {
                return CLASS.equals(kind);
            }

            @Override
            public void transform(final TransformationContext context) {
                String className = context.getTarget().asClass().name().toString();
                if (batchArtifacts.containsKey(className)) {
                    String named = batchArtifacts.get(className);
                    context.transform()
                            .add(Unremovable.class)
                            .add(Named.class, createStringValue("value", named))
                            .done();
                }
            }
        }));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void validateRepository(
            JBeretRecorder recorder,
            JBeretConfig config,
            BeanDiscoveryFinishedBuildItem beanDiscoveryFinishedBuildItem,
            List<JdbcDataSourceBuildItem> datasources) {
        switch (normalize(config.repository().type())) {
            case JBeretJdbcJobRepositoryProducer.TYPE:
                final String datasource = config.repository().jdbc().datasource();
                if (datasources.stream().noneMatch(item -> item.getName().equals(datasource))) {
                    throw new ConfigurationException("Datasource name "
                            + datasource
                            + " does not exist. Available datasources: "
                            + datasources.stream()
                                    .map(JdbcDataSourceBuildItem::getName)
                                    .collect(Collectors.joining(",")));
                }

                break;
            case JBeretInMemoryJobRepositoryProducer.TYPE:
                break;
            default:
                final DotName dotName = DotName.createSimple(JobRepository.class);
                final List<BeanInfo> beanInfos = beanDiscoveryFinishedBuildItem.beanStream().filter(
                        beanInfo -> beanInfo.hasType(dotName) && beanInfo.hasDefaultQualifiers()).collect();
                if (beanInfos.isEmpty()) {
                    throw new ConfigurationException("There is no injectable and @Default JobRepository bean");
                } else if (beanInfos.size() > 1) {
                    throw new ConfigurationException(
                            "Multiple injectable and @Default JobRepository beans are not allowed : "
                                    + beanInfos);
                }
        }
    }

    @BuildStep
    public void loadJobs(
            JBeretConfig config,
            ValidationPhaseBuildItem validationPhase,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles,
            BuildProducer<BatchJobBuildItem> batchJobs,
            BuildProducer<ValidationErrorBuildItem> validationErrors) throws Exception {

        Map<String, BeanInfo> jobBeans = new HashMap<>();
        for (BeanInfo beanInfo : validationPhase.getBeanResolver().resolveBeans(Type.create(JOB, Type.Kind.CLASS))) {
            jobBeans.put(beanInfo.getName(), beanInfo);
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        MetaInfBatchJobsJobXmlResolver jobXmlResolver = new MetaInfBatchJobsJobXmlResolver();

        ClassPathUtils.consumeAsPaths("META-INF/batch-jobs", path -> {
            Set<String> batchFilesNames = findBatchFilesFromPath(path, toPatterns(config.jobs().includes()),
                    toPatterns(config.jobs().excludes()));
            List<Job> loadedJobs = new ArrayList<>();

            batchFilesNames.forEach(jobXmlName -> {
                if (jobBeans.containsKey(jobXmlName)) {
                    validationErrors.produce(new ValidationErrorBuildItem(new AmbiguousResolutionException(
                            "Beans: " + List.of(jobBeans.get(jobXmlName).toString(), path + "/" + jobXmlName))));
                }
                Job job = ArchiveXmlLoader.loadJobXml(jobXmlName, contextClassLoader, loadedJobs, jobXmlResolver);
                job.setJobXmlName(jobXmlName);
                JobConfig jobConfig = config.job().get(jobXmlName);
                watchedFiles.produce(new HotDeploymentWatchedFileBuildItem("META-INF/batch-jobs/" + jobXmlName + ".xml"));
                watchJobScripts(job, watchedFiles);
                batchJobs.produce(new BatchJobBuildItem(job, parseCron(job, jobConfig)));
                log.debug("Processed job with ID " + job.getId() + "  from file " + jobXmlName);
            });
        });
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void registerJobs(
            RecorderContext recorderContext,
            JBeretRecorder recorder,
            JBeretConfig config,
            List<BatchJobBuildItem> batchJobs,
            List<BatchArtifactBuildItem> batchArtifacts,
            BeanContainerBuildItem beanContainer) throws Exception {
        registerNonDefaultConstructors(recorderContext);

        // TODO - Record JobSchedulerConfig - Need changes in the original class.

        List<Job> jobs = new ArrayList<>();
        for (BatchJobBuildItem batchJob : batchJobs) {
            jobs.add(batchJob.getJob());
        }

        Map<String, String> artifactsAliases = new HashMap<>();
        for (BatchArtifactBuildItem batchArtifact : batchArtifacts) {
            artifactsAliases.put(batchArtifact.getAlias(), batchArtifact.getNamed());
        }

        recorder.registerJobs(jobs, artifactsAliases, beanContainer.getValue());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem init(JBeretRecorder recorder,
            JBeretConfig config,
            ThreadPoolConfig threadPoolConfig,
            BeanContainerBuildItem beanContainer) {

        recorder.initJobOperator(config, threadPoolConfig, beanContainer.getValue());
        recorder.initScheduler(config);

        return new ServiceStartBuildItem("jberet");
    }

    @BuildStep
    public void nativeImage(BuildProducer<NativeImageResourceBuildItem> resources,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            JBeretConfig config) {
        resources.produce(new NativeImageResourceBuildItem("sql/jberet-sql.properties"));
        resources.produce(new NativeImageResourceBuildItem("sql/jberet.ddl"));
        if (JBeretJdbcJobRepositoryProducer.TYPE.equals(normalize(config.repository().type()))) {
            config.repository().jdbc().ddlFileName().map(String::trim)
                    .filter(Predicate.not(String::isEmpty))
                    .ifPresent(v -> resources.produce(new NativeImageResourceBuildItem(v)));
            config.repository().jdbc().sqlFileName().map(String::trim)
                    .filter(Predicate.not(String::isEmpty))
                    .ifPresent(v -> resources.produce(new NativeImageResourceBuildItem(v)));
        }
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(QuarkusJobScheduler.class).methods().build());
        reflectiveClasses
                .produce(ReflectiveClassBuildItem.builder(JobInstanceImpl.class).constructors().methods().fields().build());

        // Exception Serialization for persistence
        Set<String> serializationClasses = Set.of(
                Error.class.getName(),
                Throwable.class.getName(),
                Exception.class.getName(),
                RuntimeException.class.getName(),
                StackTraceElement.class.getName(),
                BatchRuntimeException.class.getName(),
                String.class.getName(),
                "java.util.Collections$EmptyList",
                "com.oracle.svm.core.jdk.UnsupportedFeatureError");

        for (String serializationClass : serializationClasses) {
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(serializationClass).serialization().build());
        }
    }

    private static void registerNonDefaultConstructors(RecorderContext recorderContext) throws Exception {
        recorderContext.registerNonDefaultConstructor(Job.class.getConstructor(String.class),
                job -> Collections.singletonList(job.getId()));

        recorderContext.registerNonDefaultConstructor(Flow.class.getConstructor(String.class),
                flow -> Collections.singletonList(flow.getId()));

        recorderContext.registerNonDefaultConstructor(Split.class.getConstructor(String.class),
                split -> Collections.singletonList(split.getId()));

        recorderContext.registerNonDefaultConstructor(Step.class.getConstructor(String.class),
                step -> Collections.singletonList(step.getId()));

        recorderContext.registerNonDefaultConstructor(RefArtifact.class.getConstructor(String.class),
                refArtifact -> Collections.singletonList(refArtifact.getRef()));

        recorderContext.registerNonDefaultConstructor(Decision.class.getConstructor(String.class, String.class),
                decision -> Stream.of(decision.getId(), decision.getRef()).collect(toList()));

        recorderContext.registerNonDefaultConstructor(Transition.class.getConstructor(String.class),
                transition -> Collections.singletonList(transition.getOn()));
        recorderContext.registerNonDefaultConstructor(Transition.End.class.getConstructor(String.class),
                end -> Collections.singletonList(end.getOn()));
        recorderContext.registerNonDefaultConstructor(Transition.Fail.class.getConstructor(String.class),
                fail -> Collections.singletonList(fail.getOn()));
        recorderContext.registerNonDefaultConstructor(Transition.Stop.class.getConstructor(String.class, String.class),
                stop -> Stream.of(stop.getOn(), stop.getRestart()).collect(toList()));
        recorderContext.registerNonDefaultConstructor(Transition.Next.class.getConstructor(String.class),
                next -> Collections.singletonList(next.getOn()));

        recorderContext.registerNonDefaultConstructor(Script.class.getConstructor(String.class, String.class, String.class),
                script -> Stream
                        .of(script.getType(), script.getSrc(),
                                script.getSrc() != null ? script.getContent(Thread.currentThread().getContextClassLoader())
                                        : script.getContent())
                        .collect(toList()));
    }

    private static Set<String> findBatchFilesFromPath(Path path, List<Pattern> includes, List<Pattern> excludes) {
        try {
            Stream<String> filePaths = Files.walk(path)
                    .filter(Files::isRegularFile)
                    .map(file -> file.getFileName().toString())
                    .filter(file -> file.endsWith(".xml"));

            if (!includes.isEmpty()) {
                filePaths = filePaths
                        .filter(filePath -> includes.stream().allMatch(pattern -> pattern.matcher(filePath).matches()));
            }

            if (!excludes.isEmpty()) {
                filePaths = filePaths
                        .filter(filePath -> excludes.stream().noneMatch(pattern -> pattern.matcher(filePath).matches()));
            }

            return filePaths.map(file -> file.substring(0, file.length() - 4)).collect(Collectors.toSet());
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    private static List<Pattern> toPatterns(Optional<List<String>> pattern) {
        return pattern.map(patterns -> patterns.stream().map(GlobUtil::toRegexPattern).map(Pattern::compile).collect(toList()))
                .orElseGet(ArrayList::new);
    }

    private static String parseCron(Job job, JobConfig jobConfig) {
        if (jobConfig == null || jobConfig.cron().isEmpty()) {
            return null;
        }

        try {
            CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
            Cron cron = parser.parse(jobConfig.cron().get());
            cron.validate();
            return jobConfig.cron().get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ConfigurationException(
                    String.format("The cron expression %s configured in %s is not valid", jobConfig.cron().get(),
                            "quarkus.jberet.job." + job.getJobXmlName() + ".cron"));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getBatchArtifacts() throws Exception {
        BatchArtifacts batchArtifacts = ArchiveXmlLoader.loadBatchXml(Thread.currentThread().getContextClassLoader());
        if (batchArtifacts == null) {
            return emptyMap();
        }

        Field refs = BatchArtifacts.class.getDeclaredField("refs");
        refs.setAccessible(true);
        Map<String, String> refsToClass = (Map<String, String>) refs.get(batchArtifacts);
        Map<String, String> classToRefs = new HashMap<>();
        for (Map.Entry<String, String> entry : refsToClass.entrySet()) {
            classToRefs.put(entry.getValue(), entry.getKey());
        }
        return classToRefs;
    }

    private static void watchJobScripts(Job job, BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) {
        for (JobElement jobElement : job.getJobElements()) {
            watchJobScripts(jobElement, watchedFiles);
        }

        for (final InheritableJobElement inheritingJobElement : job.getInheritingJobElements()) {
            watchJobScripts(inheritingJobElement, watchedFiles);
        }
    }

    private static void watchJobScripts(JobElement jobElement, BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) {
        if (jobElement instanceof Step) {
            final Step step = (Step) jobElement;

            watchJobScripts(step.getBatchlet(), watchedFiles);

            if (step.getChunk() != null) {
                watchJobScripts(step.getChunk().getReader(), watchedFiles);
                watchJobScripts(step.getChunk().getProcessor(), watchedFiles);
                watchJobScripts(step.getChunk().getWriter(), watchedFiles);
                watchJobScripts(step.getChunk().getCheckpointAlgorithm(), watchedFiles);
            }

            if (step.getPartition() != null) {
                watchJobScripts(step.getPartition().getMapper(), watchedFiles);
                watchJobScripts(step.getPartition().getCollector(), watchedFiles);
                watchJobScripts(step.getPartition().getAnalyzer(), watchedFiles);
                watchJobScripts(step.getPartition().getReducer(), watchedFiles);
            }
        }

        if (jobElement instanceof Flow) {
            final Flow flow = (Flow) jobElement;

            for (JobElement flowElement : flow.getJobElements()) {
                watchJobScripts(flowElement, watchedFiles);
            }
        }

        if (jobElement instanceof Split) {
            final Split split = (Split) jobElement;
            for (final Flow flow : split.getFlows()) {
                watchJobScripts(flow, watchedFiles);
            }
        }
    }

    private static void watchJobScripts(RefArtifact refArtifact,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) {
        if (refArtifact != null &&
                refArtifact.getScript() != null &&
                refArtifact.getScript().getSrc() != null) {
            watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(refArtifact.getScript().getSrc()));
        }
    }
}
