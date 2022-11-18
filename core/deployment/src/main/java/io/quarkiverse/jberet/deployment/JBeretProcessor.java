package io.quarkiverse.jberet.deployment;

import static io.quarkiverse.jberet.runtime.JBeretConfig.Repository.Type.JDBC;
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

import javax.enterprise.context.Dependent;
import javax.inject.Named;

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
import org.jberet.tools.MetaInfBatchJobsJobXmlResolver;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.logging.Logger;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import io.quarkiverse.jberet.runtime.JBeretConfig;
import io.quarkiverse.jberet.runtime.JBeretConfig.JobConfig;
import io.quarkiverse.jberet.runtime.JBeretProducer;
import io.quarkiverse.jberet.runtime.JBeretRecorder;
import io.quarkiverse.jberet.runtime.QuarkusJobScheduler;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceValueBuildItem;
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
    public void additionalBeans(
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) throws Exception {

        additionalBeans.produce(new AdditionalBeanBuildItem(BatchBeanProducer.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(JBeretProducer.class));

        Map<String, String> batchArtifacts = getBatchArtifacts();

        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(final AnnotationTarget.Kind kind) {
                return CLASS.equals(kind);
            }

            @Override
            public void transform(final TransformationContext context) {
                final String className = context.getTarget().asClass().name().toString();
                if (batchArtifacts.containsKey(className)) {
                    context.transform()
                            .add(Unremovable.class)
                            .add(Dependent.class)
                            .add(Named.class, createStringValue("value", batchArtifacts.get(className)))
                            .done();
                }
            }
        }));
    }

    @BuildStep
    public void loadJobs(
            JBeretConfig config,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles,
            BuildProducer<BatchJobBuildItem> batchJobs) throws Exception {

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        MetaInfBatchJobsJobXmlResolver jobXmlResolver = new MetaInfBatchJobsJobXmlResolver();

        ClassPathUtils.consumeAsPaths("META-INF/batch-jobs", path -> {
            Set<String> batchFilesNames = findBatchFilesFromPath(path, toPatterns(config.jobs().includes()),
                    toPatterns(config.jobs().excludes()));
            List<Job> loadedJobs = new ArrayList<>();

            batchFilesNames.forEach(jobXmlName -> {
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
            List<BatchJobBuildItem> batchJobs) throws Exception {
        registerNonDefaultConstructors(recorderContext);

        // TODO - Record JobSchedulerConfig - Need changes in the original class.
        recorder.registerJobs(batchJobs.stream().map(BatchJobBuildItem::getJob).collect(toList()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public RunTimeConfigurationSourceValueBuildItem config(JBeretRecorder recorder) {
        return new RunTimeConfigurationSourceValueBuildItem(recorder.config());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem init(JBeretRecorder recorder,
            JBeretConfig config,
            ThreadPoolConfig threadPoolConfig,
            BeanContainerBuildItem beanContainer,
            List<JdbcDataSourceBuildItem> datasources) {

        validateRepository(config, datasources);

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
        if (config.repository().type() == JDBC) {
            config.repository().jdbc().ddlFileName().map(String::trim)
                    .filter(Predicate.not(String::isEmpty))
                    .ifPresent(v -> resources.produce(new NativeImageResourceBuildItem(v)));
            config.repository().jdbc().sqlFileName().map(String::trim)
                    .filter(Predicate.not(String::isEmpty))
                    .ifPresent(v -> resources.produce(new NativeImageResourceBuildItem(v)));
        }
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, QuarkusJobScheduler.class));
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

    private static void validateRepository(
            final JBeretConfig config,
            final List<JdbcDataSourceBuildItem> datasources) {

        if (JDBC.equals(config.repository().type())) {
            final String datasource = config.repository().jdbc().datasource();
            if (datasources.stream().noneMatch(item -> item.getName().equals(datasource))) {
                throw new ConfigurationException("Datasource name " +
                        datasource +
                        " does not exist. Available datasources: " +
                        datasources.stream()
                                .map(JdbcDataSourceBuildItem::getName)
                                .collect(Collectors.joining(",")));
            }
        }
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
