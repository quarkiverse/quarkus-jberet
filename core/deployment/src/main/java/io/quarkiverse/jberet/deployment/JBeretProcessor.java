package io.quarkiverse.jberet.deployment;

import static io.quarkiverse.jberet.deployment.DotNames.JOB;
import static io.quarkiverse.jberet.deployment.DotNames.JOB_ELEMENTS;
import static io.quarkiverse.jberet.deployment.DotNames.JOB_REPOSITORY;
import static java.util.stream.Collectors.toList;
import static org.jberet.spi.JobXmlResolver.DEFAULT_PATH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.batch.operations.BatchRuntimeException;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;

import org.jberet.cdi.JobScoped;
import org.jberet.cdi.PartitionScoped;
import org.jberet.cdi.StepScoped;
import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.creation.BatchBeanProducer;
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
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.spi.JobXmlResolver;
import org.jberet.tools.ChainedJobXmlResolver;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
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
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanResolver;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.configuration.ConfigurationException;

class JBeretProcessor {
    private static final Logger log = Logger.getLogger("io.quarkiverse.jberet");

    @BuildStep
    void registerExtension(BuildProducer<FeatureBuildItem> feature, BuildProducer<CapabilityBuildItem> capability) {
        feature.produce(new FeatureBuildItem("jberet"));
    }

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        indexDependencies.produce(new IndexDependencyBuildItem("org.jberet", null));
        indexDependencies.produce(new IndexDependencyBuildItem("jakarta.batch", null));
    }

    /**
     * Prevent JobOperatorContext$DefaultHolder from eagerly initializing because it depends on a ServiceLoader
     * entry for the BatchRuntime, which we don't use. With this trigger turned off, it won't ever be initialized.
     */
    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedDefaultHolder() {
        return new RuntimeInitializedClassBuildItem("org.jberet.spi.JobOperatorContext$DefaultHolder");
    }

    @BuildStep
    void batchScopes(ContextRegistrationPhaseBuildItem c, BuildProducer<ContextConfiguratorBuildItem> v) {
        v.produce(new ContextConfiguratorBuildItem(
                c.getContext().configure(JobScoped.class).contextClass(QuarkusJobScopedContextImpl.class),
                c.getContext().configure(StepScoped.class).contextClass(QuarkusStepScopedContextImpl.class),
                c.getContext().configure(PartitionScoped.class).contextClass(QuarkusPartitionScopedContextImpl.class)));
    }

    @BuildStep
    void config(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(JBeretConfigSourceFactoryBuilder.class.getName()));
    }

    @BuildStep
    void additionalBeans(
            JBeretConfig config,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        additionalBeans.produce(new AdditionalBeanBuildItem(BatchBeanProducer.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(JBeretProducer.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(JobsProducer.class));

        switch (config.repository().type()) {
            case JBeretInMemoryJobRepositoryProducer.TYPE:
                additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(JBeretInMemoryJobRepositoryProducer.class));
                break;
            case JBeretJdbcJobRepositoryProducer.TYPE:
                additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(JBeretJdbcJobRepositoryProducer.class));
                break;
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void validateRepository(
            JBeretRecorder recorder,
            JBeretConfig config,
            BeanDiscoveryFinishedBuildItem beanDiscoveryFinishedBuildItem,
            List<JdbcDataSourceBuildItem> datasources) {
        switch (config.repository().type()) {
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
                final List<BeanInfo> beanInfos = beanDiscoveryFinishedBuildItem.beanStream().filter(
                        beanInfo -> beanInfo.hasType(JOB_REPOSITORY) && beanInfo.hasDefaultQualifiers()).collect();
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
    void loadJobs(
            JBeretBuildTimeConfig buildTimeConfig,
            JBeretConfig config,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles,
            BuildProducer<BatchJobBuildItem> batchJobs) throws Exception {

        List<Job> jobs = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        JobXmlResolver jobXmlResolver = new ChainedJobXmlResolver(ServiceLoader.load(JobXmlResolver.class, classLoader),
                new QuarkusJobXmlResolver(buildTimeConfig, classLoader));

        for (String jobXmlName : jobXmlResolver.getJobXmlNames(classLoader)) {
            Job job = ArchiveXmlLoader.loadJobXml(jobXmlName, classLoader, jobs, jobXmlResolver);
            job.setJobXmlName(jobXmlName);
            JobConfig jobConfig = config.job().get(jobXmlName);
            watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(DEFAULT_PATH + jobXmlName + ".xml"));
            batchJobs.produce(new BatchJobBuildItem(job, parseCron(job, jobConfig)));
            log.debug("Processed job with ID " + job.getId() + "  from file " + jobXmlName);
        }
    }

    @BuildStep
    void processRefArtifacts(
            CombinedIndexBuildItem combinedIndex,
            List<BatchJobBuildItem> batchJobs,
            BuildProducer<RefArtifactBuildItem> refArtifacts,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) {

        Map<String, ClassInfo> refsInfos = new HashMap<>();
        for (DotName component : JOB_ELEMENTS) {
            refsInfos.putAll(getRefsClassInfos(combinedIndex.getIndex().getAllKnownImplementations(component)));
        }

        for (BatchJobBuildItem batchJob : batchJobs) {
            processJob(batchJob.getJob(), refsInfos, refArtifacts, watchedFiles);
        }
    }

    @BuildStep
    void additionalBeansRefArtifacts(
            List<RefArtifactBuildItem> refArtifacts,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        for (RefArtifactBuildItem refArtifact : refArtifacts) {
            ClassInfo resolvedArtifact = refArtifact.getResolvedArtifact();
            if (resolvedArtifact != null) {
                additionalBeans.produce(AdditionalBeanBuildItem.builder()
                        .addBeanClass(resolvedArtifact.name().toString())
                        .setUnremovable()
                        .build());
            }
        }
    }

    @BuildStep
    void validateJobs(
            JBeretConfig config,
            List<BatchJobBuildItem> batchJobs,
            List<RefArtifactBuildItem> refArtifacts,
            ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {

        // Validate Job Config
        Set<String> jobNames = batchJobs.stream().map(BatchJobBuildItem::getJob).map(Job::getJobXmlName)
                .collect(Collectors.toSet());
        for (String jobName : config.job().keySet()) {
            if (!jobNames.contains(jobName)) {
                log.warn("Configured job name " + jobName
                        + " does not exist. Please check if a Job definition XML file exists with the job name " + jobName
                        + " in META-INF/batch-jobs");
            }
        }

        BeanResolver beanResolver = validationPhase.getBeanResolver();

        Map<String, BeanInfo> jobBeans = new HashMap<>();
        for (BeanInfo beanInfo : beanResolver
                .resolveBeans(Type.create(JOB, Type.Kind.CLASS))) {
            jobBeans.put(beanInfo.getName(), beanInfo);
            // TODO - Add CDI Jobs to the jobs maps so they can be merged with the ones coming from XML?
        }

        for (BatchJobBuildItem batchJob : batchJobs) {
            String jobXmlName = batchJob.getJob().getJobXmlName();
            if (jobBeans.containsKey(jobXmlName)) {
                validationErrors.produce(new ValidationErrorBuildItem(new AmbiguousResolutionException(
                        "Beans: " + List.of(jobBeans.get(jobXmlName).toString(), DEFAULT_PATH + jobXmlName + ".xml"))));
            }
        }

        // Validate that there is a matching bean to each JobElement with a ref.
        for (RefArtifactBuildItem refArtifact : refArtifacts) {
            // Some elements may be undetermined if they reference a parameter expression
            ClassInfo resolvedArtifact = refArtifact.getResolvedArtifact();
            if (resolvedArtifact == null) {
                log.warn("Unable to validate ref artifact " + refArtifact.getRefArtifact().getRef());
                continue;
            }

            Set<BeanInfo> candidates = beanResolver
                    .resolveBeans(Type.create(resolvedArtifact.name(), Kind.CLASS));
            if (candidates.isEmpty()) {
                validationErrors.produce(new ValidationErrorBuildItem(new UnsatisfiedResolutionException(
                        "RefArtifact " + refArtifact.getResolvedArtifact().name() + " not found")));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerJobs(
            RecorderContext recorderContext,
            JBeretRecorder recorder,
            List<BatchJobBuildItem> batchJobs,
            BeanContainerBuildItem beanContainer) throws Exception {
        registerNonDefaultConstructors(recorderContext);

        // TODO - Record JobSchedulerConfig - Need changes in the original class.

        List<Job> jobs = new ArrayList<>();
        for (BatchJobBuildItem batchJob : batchJobs) {
            jobs.add(batchJob.getJob());
        }

        recorder.registerJobs(jobs, beanContainer.getValue());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem init(JBeretRecorder recorder,
            BeanContainerBuildItem beanContainer) {

        recorder.initJobOperator(beanContainer.getValue());
        recorder.initScheduler();

        return new ServiceStartBuildItem("jberet");
    }

    @BuildStep
    void nativeImage(BuildProducer<NativeImageResourceBuildItem> resources,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            JBeretConfig config) {
        resources.produce(new NativeImageResourceBuildItem("sql/jberet-sql.properties"));
        resources.produce(new NativeImageResourceBuildItem("sql/jberet.ddl"));
        if (JBeretJdbcJobRepositoryProducer.TYPE.equals(config.repository().type())) {
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
            throw new ConfigurationException(
                    String.format("The cron expression %s configured in %s is not valid", jobConfig.cron().get(),
                            "quarkus.jberet.job." + job.getJobXmlName() + ".cron"));
        }
    }

    private static Map<String, ClassInfo> getRefsClassInfos(Collection<ClassInfo> refArtifacts) {
        // Map with Bean names to discovered Batch components
        Map<String, ClassInfo> refNames = new HashMap<>();
        for (ClassInfo refArtifact : refArtifacts) {
            AnnotationInstance named = refArtifact.declaredAnnotation(DotNames.NAMED);
            refNames.put(refArtifact.name().toString(), refArtifact);
            if (named != null && named.value() != null && named.value().value() != null
                    && !named.value().value().toString().isEmpty()) {
                refNames.put(named.value().value().toString(), refArtifact);
            } else {
                StringBuilder defaultName = new StringBuilder();
                defaultName.append(DotNames.simpleName(refArtifact.simpleName()));
                defaultName.setCharAt(0, Character.toLowerCase(defaultName.charAt(0)));
                refNames.put(defaultName.toString(), refArtifact);
            }
        }
        return refNames;
    }

    private static void processJob(
            Job job,
            Map<String, ClassInfo> refsInfos,
            BuildProducer<RefArtifactBuildItem> refArtifacts,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) {

        if (job.getListeners() != null) {
            for (RefArtifact listener : job.getListeners().getListeners()) {
                processJob(listener, refsInfos, refArtifacts, watchedFiles);
            }
        }

        for (JobElement jobElement : job.getJobElements()) {
            processJob(jobElement, refsInfos, refArtifacts, watchedFiles);
        }

        for (InheritableJobElement inheritingJobElement : job.getInheritingJobElements()) {
            processJob(inheritingJobElement, refsInfos, refArtifacts, watchedFiles);
        }
    }

    private static void processJob(
            JobElement jobElement,
            Map<String, ClassInfo> refsInfos,
            BuildProducer<RefArtifactBuildItem> refArtifacts,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) {

        if (jobElement instanceof Step step) {
            processJob(step.getBatchlet(), refsInfos, refArtifacts, watchedFiles);

            if (step.getListeners() != null) {
                for (RefArtifact listener : step.getListeners().getListeners()) {
                    processJob(listener, refsInfos, refArtifacts, watchedFiles);
                }
            }

            if (step.getChunk() != null) {
                processJob(step.getChunk().getReader(), refsInfos, refArtifacts, watchedFiles);
                processJob(step.getChunk().getProcessor(), refsInfos, refArtifacts, watchedFiles);
                processJob(step.getChunk().getWriter(), refsInfos, refArtifacts, watchedFiles);
                processJob(step.getChunk().getCheckpointAlgorithm(), refsInfos, refArtifacts, watchedFiles);
            }

            if (step.getPartition() != null) {
                processJob(step.getPartition().getMapper(), refsInfos, refArtifacts, watchedFiles);
                processJob(step.getPartition().getCollector(), refsInfos, refArtifacts, watchedFiles);
                processJob(step.getPartition().getAnalyzer(), refsInfos, refArtifacts, watchedFiles);
                processJob(step.getPartition().getReducer(), refsInfos, refArtifacts, watchedFiles);
            }
        }

        if (jobElement instanceof Flow flow) {
            for (JobElement flowElement : flow.getJobElements()) {
                processJob(flowElement, refsInfos, refArtifacts, watchedFiles);
            }
        }

        if (jobElement instanceof Split split) {
            for (Flow flow : split.getFlows()) {
                processJob(flow, refsInfos, refArtifacts, watchedFiles);
            }
        }

        if (jobElement instanceof Decision decision) {
            processJob(new RefArtifact(decision.getRef()), refsInfos, refArtifacts, watchedFiles);
        }
    }

    private static void processJob(
            RefArtifact refArtifact,
            Map<String, ClassInfo> refsInfos,
            BuildProducer<RefArtifactBuildItem> refArtifacts,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) {

        if (refArtifact != null) {
            if (refArtifact.getRef() != null && !refArtifact.getRef().isEmpty()) {
                // May not be able to resolve an artifact, if the ref is a job parameter expression
                ClassInfo resolvedArtifact = refsInfos.get(refArtifact.getRef());
                refArtifacts.produce(new RefArtifactBuildItem(refArtifact, resolvedArtifact));
            }
            if (refArtifact.getScript() != null && refArtifact.getScript().getSrc() != null) {
                watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(refArtifact.getScript().getSrc()));
            }
        }
    }
}
