package io.quarkiverse.jberet.runtime.dev.ui;

import static jakarta.batch.runtime.BatchStatus.ABANDONED;
import static jakarta.batch.runtime.BatchStatus.FAILED;
import static jakarta.batch.runtime.BatchStatus.STARTED;
import static jakarta.batch.runtime.BatchStatus.STARTING;
import static jakarta.batch.runtime.BatchStatus.STOPPED;
import static jakarta.batch.runtime.BatchStatus.STOPPING;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.Metric;
import jakarta.batch.runtime.StepExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jberet.job.model.Flow;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.Split;
import org.jberet.job.model.Step;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.schedule.JobSchedule;
import org.jberet.schedule.JobScheduleConfig;
import org.jboss.logging.Logger;

import io.quarkiverse.jberet.runtime.JBeretRuntimeConfig;
import io.quarkiverse.jberet.runtime.QuarkusJobOperator;
import io.quarkiverse.jberet.runtime.QuarkusJobScheduler;
import io.quarkiverse.jberet.runtime.dev.ui.BatchArtifacts.ArtifactInfo;
import io.quarkiverse.jberet.runtime.dev.ui.JBeretDevEvents.BatchEvent;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.assistant.runtime.dev.Assistant;
import io.quarkus.runtime.annotations.DevMCPEnableByDefault;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Dev UI and Dev MCP JSON-RPC methods to inspect and operate batch Jobs.
 */
@ApplicationScoped
public class JBeretJsonRPCService {
    private static final Logger LOG = Logger.getLogger(JBeretJsonRPCService.class);
    private static final int EXECUTIONS_LIMIT = 100;

    @Inject
    QuarkusJobOperator jobOperator;
    @Inject
    JBeretRuntimeConfig config;
    @Inject
    QuarkusJobScheduler jobScheduler;
    @Inject
    InjectableInstance<Scheduler> scheduler;
    @Inject
    JBeretDevEvents events;
    @Inject
    Optional<Assistant> assistant;

    @JsonRpcDescription("Get the number of batch jobs in the application")
    public int getJobCount() {
        return jobOperator.getJobs().size();
    }

    @JsonRpcDescription("Get the batch jobs in the application, with their steps, cron schedule, configured parameters and latest execution")
    @DevMCPEnableByDefault
    public List<JobInfo> getJobs() {
        List<JobInfo> jobs = new ArrayList<>();
        for (Map.Entry<String, Job> entry : new TreeMap<>(jobOperator.getJobs()).entrySet()) {
            String name = entry.getKey();
            Job job = entry.getValue();
            JBeretRuntimeConfig.JobConfig jobConfig = config.job().get(name);

            List<String> steps = new ArrayList<>();
            collectSteps(job.getJobElements(), steps);

            ExecutionInfo lastExecution = null;
            List<Long> executionIds = jobOperator.getJobRepository().getJobExecutionsByJob(job.getId(), 1);
            if (!executionIds.isEmpty()) {
                JobExecution execution = jobOperator.getJobExecution(executionIds.get(0));
                lastExecution = toExecutionInfo(execution, instanceId(execution), true);
            }

            jobs.add(new JobInfo(
                    name,
                    job.getId(),
                    job.getRestartableBoolean(),
                    steps,
                    jobConfig.cron().orElse(null),
                    new TreeMap<>(jobConfig.params()),
                    lastExecution));
        }
        return jobs;
    }

    @JsonRpcDescription("Get the latest executions of batch jobs (newest first), with their status, parameters and the operations allowed on each one")
    @DevMCPEnableByDefault
    public List<ExecutionInfo> getExecutions(
            @JsonRpcDescription("The name of the job to get the executions for, or empty to get the executions of all jobs") String jobName) {
        Set<String> jobIds = new LinkedHashSet<>();
        if (jobName == null || jobName.isBlank()) {
            jobIds.addAll(jobIds());
        } else {
            Job job = jobOperator.getJobs().get(jobName);
            jobIds.add(job != null ? job.getId() : jobName);
        }

        List<JobExecution> executions = new ArrayList<>();
        for (String jobId : jobIds) {
            for (Long executionId : jobOperator.getJobRepository().getJobExecutionsByJob(jobId, EXECUTIONS_LIMIT)) {
                executions.add(jobOperator.getJobExecution(executionId));
            }
        }
        executions.sort(Comparator.comparingLong(JobExecution::getExecutionId).reversed());
        if (executions.size() > EXECUTIONS_LIMIT) {
            executions = executions.subList(0, EXECUTIONS_LIMIT);
        }

        // Executions are sorted from newest to oldest, so the first execution found for an instance is its most recent
        Set<Long> seenInstances = new HashSet<>();
        List<ExecutionInfo> result = new ArrayList<>(executions.size());
        for (JobExecution execution : executions) {
            long instanceId = instanceId(execution);
            result.add(toExecutionInfo(execution, instanceId, seenInstances.add(instanceId)));
        }
        return result;
    }

    @JsonRpcDescription("Get the step executions of a batch job execution, with their status, metrics (read, write, commit, rollback, filter and skip counts) and failure details")
    @DevMCPEnableByDefault
    public List<StepExecutionInfo> getStepExecutions(
            @JsonRpcDescription("The id of the job execution") long executionId) {
        List<StepExecutionInfo> steps = new ArrayList<>();
        for (StepExecution stepExecution : jobOperator.getStepExecutions(executionId)) {
            Map<String, Long> metrics = new TreeMap<>();
            if (stepExecution.getMetrics() != null) {
                for (Metric metric : stepExecution.getMetrics()) {
                    metrics.put(metric.getType().name(), metric.getValue());
                }
            }

            String exception = null;
            if (stepExecution instanceof AbstractStepExecution abstractStepExecution
                    && abstractStepExecution.getException() != null) {
                StringWriter stackTrace = new StringWriter();
                abstractStepExecution.getException().printStackTrace(new PrintWriter(stackTrace));
                exception = stackTrace.toString();
            }

            steps.add(new StepExecutionInfo(
                    stepExecution.getStepExecutionId(),
                    stepExecution.getStepName(),
                    name(stepExecution.getBatchStatus()),
                    stepExecution.getExitStatus(),
                    time(stepExecution.getStartTime()),
                    time(stepExecution.getEndTime()),
                    metrics,
                    exception));
        }
        steps.sort(Comparator.comparingLong(StepExecutionInfo::stepExecutionId));
        return steps;
    }

    @JsonRpcDescription("Start a new execution of a batch job. Parameters configured for the job are used unless overridden")
    public ActionResult startJob(
            @JsonRpcDescription("The name of the job to start") String jobName,
            @JsonRpcDescription("The job parameters, as key / value pairs") Map<String, Object> parameters) {
        try {
            long executionId = jobOperator.start(jobName, toProperties(parameters));
            LOG.infof("Started job '%s' with execution id %d via Dev UI", jobName, executionId);
            return ActionResult.success("Started job " + jobName + " with execution id " + executionId, executionId);
        } catch (Exception e) {
            return ActionResult.failure("Failed to start job " + jobName + ": " + e.getMessage());
        }
    }

    @JsonRpcDescription("Restart a failed or stopped batch job execution. Only the most recent execution of a job instance can be restarted")
    public ActionResult restartExecution(
            @JsonRpcDescription("The id of the job execution to restart") long executionId,
            @JsonRpcDescription("The restart parameters, as key / value pairs") Map<String, Object> parameters) {
        try {
            long newExecutionId = jobOperator.restart(executionId, toProperties(parameters));
            LOG.infof("Restarted job execution %d with execution id %d via Dev UI", executionId, newExecutionId);
            return ActionResult.success(
                    "Restarted execution " + executionId + " with execution id " + newExecutionId, newExecutionId);
        } catch (Exception e) {
            return ActionResult.failure("Failed to restart execution " + executionId + ": " + e.getMessage());
        }
    }

    @JsonRpcDescription("Request a running batch job execution to stop")
    public ActionResult stopExecution(
            @JsonRpcDescription("The id of the job execution to stop") long executionId) {
        try {
            jobOperator.stop(executionId);
            LOG.infof("Requested stop of job execution %d via Dev UI", executionId);
            return ActionResult.success("Requested stop of execution " + executionId, executionId);
        } catch (Exception e) {
            return ActionResult.failure("Failed to stop execution " + executionId + ": " + e.getMessage());
        }
    }

    @JsonRpcDescription("Abandon a batch job execution that is not running, so that it can no longer be restarted")
    public ActionResult abandonExecution(
            @JsonRpcDescription("The id of the job execution to abandon") long executionId) {
        try {
            jobOperator.abandon(executionId);
            LOG.infof("Abandoned job execution %d via Dev UI", executionId);
            return ActionResult.success("Abandoned execution " + executionId, executionId);
        } catch (Exception e) {
            return ActionResult.failure("Failed to abandon execution " + executionId + ": " + e.getMessage());
        }
    }

    @JsonRpcDescription("Get the flow diagram of a batch job in Graphviz DOT format, with its steps, decisions, flows, splits and transitions")
    @DevMCPEnableByDefault
    public String getJobDiagram(
            @JsonRpcDescription("The name of the job") String jobName) {
        Job job = jobOperator.getJobs().get(jobName);
        return job != null ? JobDiagram.toDot(job, null) : null;
    }

    @JsonRpcDescription("Get the flow diagram of a batch job execution in Graphviz DOT format, with the steps colored by their status in the execution")
    @DevMCPEnableByDefault
    public String getExecutionDiagram(
            @JsonRpcDescription("The id of the job execution") long executionId) {
        JobExecution execution = jobOperator.getJobExecution(executionId);
        Job job = findJobById(execution.getJobName());
        if (job == null) {
            return null;
        }
        // Executions are sorted by id, so a step executed more than once (in a loop) keeps the latest status
        Map<String, String> stepStatuses = new HashMap<>();
        List<StepExecution> stepExecutions = new ArrayList<>(jobOperator.getStepExecutions(executionId));
        stepExecutions.sort(Comparator.comparingLong(StepExecution::getStepExecutionId));
        for (StepExecution stepExecution : stepExecutions) {
            stepStatuses.put(stepExecution.getStepName(), name(stepExecution.getBatchStatus()));
        }
        return JobDiagram.toDot(job, stepStatuses);
    }

    @JsonRpcDescription("Get the number of batch job schedules")
    public int getScheduleCount() {
        return isSchedulerStarted() ? jobScheduler.getJobSchedules().size() : 0;
    }

    @JsonRpcDescription("Get the batch job schedules, from the cron configuration of each job and the ones created with the JobScheduler, with their next and previous fire times")
    @DevMCPEnableByDefault
    public SchedulesInfo getSchedules() {
        if (!isSchedulerStarted()) {
            return new SchedulesInfo(false, List.of());
        }

        Scheduler activeScheduler = scheduler.getActive();
        List<ScheduleInfo> schedules = new ArrayList<>();
        for (JobSchedule jobSchedule : jobScheduler.getJobSchedules()) {
            JobScheduleConfig scheduleConfig = jobSchedule.getJobScheduleConfig();
            String jobName = scheduleConfig.getJobName();

            String cron = null;
            if (scheduleConfig.getScheduleExpression() != null) {
                cron = config.job().get(jobName).cron()
                        .orElseGet(() -> QuarkusJobScheduler.toCronExpression(scheduleConfig.getScheduleExpression()));
            }

            Trigger trigger = activeScheduler.getScheduledJob(jobSchedule.getId());
            schedules.add(new ScheduleInfo(
                    jobSchedule.getId(),
                    jobName,
                    cron,
                    scheduleConfig.getInitialDelay(),
                    scheduleConfig.getInterval(),
                    toMap(scheduleConfig.getJobParameters()),
                    activeScheduler.isPaused(jobSchedule.getId()),
                    trigger != null ? time(trigger.getNextFireTime()) : null,
                    trigger != null ? time(trigger.getPreviousFireTime()) : null,
                    jobSchedule.getJobExecutionIds() != null ? new ArrayList<>(jobSchedule.getJobExecutionIds())
                            : List.of()));
        }
        schedules.sort(Comparator.comparing(ScheduleInfo::jobName).thenComparing(ScheduleInfo::id));
        return new SchedulesInfo(true, schedules);
    }

    @JsonRpcDescription("Pause a batch job schedule, so that it does not start the job until resumed")
    public ActionResult pauseSchedule(
            @JsonRpcDescription("The id of the schedule") String scheduleId) {
        try {
            scheduler.getActive().pause(scheduleId);
            LOG.infof("Paused job schedule %s via Dev UI", scheduleId);
            return ActionResult.success("Paused schedule " + scheduleId);
        } catch (Exception e) {
            return ActionResult.failure("Failed to pause schedule " + scheduleId + ": " + e.getMessage());
        }
    }

    @JsonRpcDescription("Resume a paused batch job schedule")
    public ActionResult resumeSchedule(
            @JsonRpcDescription("The id of the schedule") String scheduleId) {
        try {
            scheduler.getActive().resume(scheduleId);
            LOG.infof("Resumed job schedule %s via Dev UI", scheduleId);
            return ActionResult.success("Resumed schedule " + scheduleId);
        } catch (Exception e) {
            return ActionResult.failure("Failed to resume schedule " + scheduleId + ": " + e.getMessage());
        }
    }

    @JsonRpcDescription("Cancel a batch job schedule. A schedule from the cron configuration is created again when the application restarts")
    public ActionResult cancelSchedule(
            @JsonRpcDescription("The id of the schedule") String scheduleId) {
        try {
            if (!jobScheduler.cancel(scheduleId)) {
                return ActionResult.failure("Schedule " + scheduleId + " not found");
            }
            LOG.infof("Cancelled job schedule %s via Dev UI", scheduleId);
            return ActionResult.success("Cancelled schedule " + scheduleId);
        } catch (Exception e) {
            return ActionResult.failure("Failed to cancel schedule " + scheduleId + ": " + e.getMessage());
        }
    }

    @JsonRpcDescription("Get the latest events of job and step executions (started and ended, with the step metrics and failures), oldest first")
    @DevMCPEnableByDefault
    public List<BatchEvent> getRecentEvents() {
        return events.recent();
    }

    public Multi<BatchEvent> streamEvents() {
        return events.stream();
    }

    @JsonRpcDescription("Get the batch artifacts (batchlets, readers, processors, writers, deciders, listeners, partition artifacts) referenced by the batch jobs, how each reference resolves to a class, and which jobs and steps use them. Also lists named batch artifacts available to use, but not used by any job. An UNRESOLVED artifact makes the job fail when it runs")
    @DevMCPEnableByDefault
    public List<ArtifactInfo> getBatchArtifacts() {
        return BatchArtifacts.collect(jobOperator);
    }

    // JSON-RPC methods that return a CompletionStage are called on the event loop, where the transactional Job
    // repository cannot be used. A Uni is subscribed on a worker thread.
    public Uni<Map<String, String>> explainFailure(long executionId) {
        return Uni.createFrom().completionStage(() -> assistExplainFailure(executionId));
    }

    public Uni<Map<String, String>> generateJob(String description) {
        return Uni.createFrom().completionStage(() -> assistGenerateJob(description));
    }

    private CompletionStage<Map<String, String>> assistExplainFailure(final long executionId) {
        if (assistant.isEmpty()) {
            return CompletableFuture.failedStage(new IllegalStateException("The Dev Assistant is not available"));
        }

        JobExecution execution = jobOperator.getJobExecution(executionId);
        Job job = findJobById(execution.getJobName());
        StringBuilder steps = new StringBuilder();
        for (StepExecutionInfo step : getStepExecutions(executionId)) {
            steps.append("- Step `").append(step.stepName()).append("`: batch status ").append(step.batchStatus())
                    .append(", exit status ").append(step.exitStatus()).append(", metrics ").append(step.metrics())
                    .append("\n");
            if (step.exception() != null) {
                steps.append("  Exception:\n```\n").append(truncate(step.exception(), 6000)).append("\n```\n");
            }
        }

        List<String> artifacts = new ArrayList<>();
        for (ArtifactInfo artifact : BatchArtifacts.collect(jobOperator)) {
            if (artifact.usages().stream().anyMatch(usage -> job != null && usage.jobName().equals(jobName(job)))) {
                artifacts.add("- `" + artifact.ref() + "` (" + String.join(", ", artifact.types()) + "): "
                        + (artifact.className() != null ? artifact.className() : artifact.status()));
            }
        }

        return assistant.get().assistBuilder()
                .systemMessage(ASSISTANT_SYSTEM_MESSAGE)
                .userMessage(EXPLAIN_FAILURE)
                .addVariable("jobName", execution.getJobName())
                .addVariable("executionId", String.valueOf(executionId))
                .addVariable("batchStatus", name(execution.getBatchStatus()))
                .addVariable("exitStatus", String.valueOf(execution.getExitStatus()))
                .addVariable("parameters", String.valueOf(toMap(execution.getJobParameters())))
                .addVariable("jobDefinition", job != null ? jobDefinition(job) : "Unknown")
                .addVariable("artifacts", artifacts.isEmpty() ? "Unknown" : String.join("\n", artifacts))
                .addVariable("steps", steps.isEmpty() ? "No steps executed" : steps.toString())
                .responseType(ExplainFailureResponse.class)
                .assist();
    }

    private CompletionStage<Map<String, String>> assistGenerateJob(final String description) {
        if (assistant.isEmpty()) {
            return CompletableFuture.failedStage(new IllegalStateException("The Dev Assistant is not available"));
        }

        StringBuilder artifacts = new StringBuilder();
        for (ArtifactInfo artifact : BatchArtifacts.collect(jobOperator)) {
            if (BatchArtifacts.RESOLVED.equals(artifact.status())) {
                artifacts.append("- `").append(artifact.ref()).append("` (").append(String.join(", ", artifact.types()))
                        .append("): ").append(artifact.className()).append(artifact.builtIn() ? " (built-in)" : "")
                        .append("\n");
            }
        }

        return assistant.get().assistBuilder()
                .systemMessage(ASSISTANT_SYSTEM_MESSAGE)
                .userMessage(GENERATE_JOB)
                .addVariable("description", description)
                .addVariable("jobs", String.join(", ", new TreeMap<>(jobOperator.getJobs()).keySet()))
                .addVariable("artifacts", artifacts.isEmpty() ? "None" : artifacts.toString())
                .responseType(GenerateJobResponse.class)
                .assist();
    }

    private Set<String> jobIds() {
        Set<String> jobIds = new LinkedHashSet<>();
        for (Job job : jobOperator.getJobs().values()) {
            jobIds.add(job.getId());
        }
        return jobIds;
    }

    private Job findJobById(final String jobId) {
        for (Job job : jobOperator.getJobs().values()) {
            if (job.getId().equals(jobId)) {
                return job;
            }
        }
        return null;
    }

    private String jobName(final Job job) {
        for (Map.Entry<String, Job> entry : jobOperator.getJobs().entrySet()) {
            if (entry.getValue() == job) {
                return entry.getKey();
            }
        }
        return job.getId();
    }

    /**
     * The Job XML, if the Job is defined in XML, or the flow diagram of the Job otherwise.
     */
    private String jobDefinition(final Job job) {
        String xmlName = job.getJobXmlName() != null ? job.getJobXmlName() : jobName(job);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream xml = classLoader.getResourceAsStream("META-INF/batch-jobs/" + xmlName + ".xml")) {
            if (xml != null) {
                return "Job XML (META-INF/batch-jobs/" + xmlName + ".xml):\n```xml\n"
                        + new String(xml.readAllBytes(), StandardCharsets.UTF_8) + "\n```";
            }
        } catch (IOException e) {
            // Use the diagram
        }
        return "The Job is defined with a CDI bean (not XML). Job flow in Graphviz DOT format:\n```dot\n"
                + JobDiagram.toDot(job, null) + "\n```";
    }

    private static String truncate(final String value, final int length) {
        return value.length() <= length ? value : value.substring(0, length) + "\n...";
    }

    private boolean isSchedulerStarted() {
        return scheduler.isResolvable() && scheduler.getActive().isStarted();
    }

    private long instanceId(final JobExecution execution) {
        return jobOperator.getJobInstance(execution.getExecutionId()).getInstanceId();
    }

    private ExecutionInfo toExecutionInfo(
            final JobExecution execution,
            final long instanceId,
            final boolean mostRecentOfInstance) {
        BatchStatus status = execution.getBatchStatus();
        boolean running = status == STARTING || status == STARTED;
        Job job = findJobById(execution.getJobName());
        boolean jobRestartable = job == null || job.getRestartableBoolean();

        return new ExecutionInfo(
                execution.getExecutionId(),
                instanceId,
                execution.getJobName(),
                name(status),
                execution.getExitStatus(),
                time(execution.getCreateTime()),
                time(execution.getStartTime()),
                time(execution.getEndTime()),
                toMap(execution.getJobParameters()),
                running,
                mostRecentOfInstance && jobRestartable && (status == FAILED || status == STOPPED),
                !running && status != STOPPING && status != ABANDONED);
    }

    private static void collectSteps(final List<JobElement> elements, final List<String> steps) {
        for (JobElement element : elements) {
            if (element instanceof Step) {
                steps.add(element.getId());
            } else if (element instanceof Flow flow) {
                collectSteps(flow.getJobElements(), steps);
            } else if (element instanceof Split split) {
                for (Flow flow : split.getFlows()) {
                    collectSteps(flow.getJobElements(), steps);
                }
            }
        }
    }

    private static Properties toProperties(final Map<String, Object> parameters) {
        Properties properties = new Properties();
        if (parameters != null) {
            for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
                if (parameter.getKey() != null && !parameter.getKey().isBlank() && parameter.getValue() != null) {
                    properties.setProperty(parameter.getKey().trim(), String.valueOf(parameter.getValue()));
                }
            }
        }
        return properties;
    }

    private static Map<String, String> toMap(final Properties properties) {
        Map<String, String> map = new TreeMap<>();
        if (properties != null) {
            for (String name : properties.stringPropertyNames()) {
                // Internal parameter added by JBeret, and recovered by JBeret itself on restart
                if (!Job.JOB_XML_NAME.equals(name)) {
                    map.put(name, properties.getProperty(name));
                }
            }
        }
        return map;
    }

    private static String name(final BatchStatus status) {
        return status != null ? status.name() : null;
    }

    private static Long time(final Date date) {
        return date != null ? date.getTime() : null;
    }

    private static Long time(final Instant instant) {
        return instant != null ? instant.toEpochMilli() : null;
    }

    public record JobInfo(
            String name,
            String id,
            boolean restartable,
            List<String> steps,
            String cron,
            Map<String, String> parameters,
            ExecutionInfo lastExecution) {
    }

    public record ExecutionInfo(
            long executionId,
            long instanceId,
            String jobName,
            String batchStatus,
            String exitStatus,
            Long createTime,
            Long startTime,
            Long endTime,
            Map<String, String> parameters,
            boolean stoppable,
            boolean restartable,
            boolean abandonable) {
    }

    public record StepExecutionInfo(
            long stepExecutionId,
            String stepName,
            String batchStatus,
            String exitStatus,
            Long startTime,
            Long endTime,
            Map<String, Long> metrics,
            String exception) {
    }

    public record ExplainFailureResponse(String markdown) {
    }

    public record GenerateJobResponse(String jobName, String jobXml, String java, String configuration, String markdown) {
    }

    private static final String ASSISTANT_SYSTEM_MESSAGE = """
            You are an expert in Jakarta Batch (JSR-352), the JBeret implementation, and the Quarkus JBeret extension
            (quarkus-jberet). In Quarkus JBeret:
            - Job XML files go in src/main/resources/META-INF/batch-jobs/<job-name>.xml, and the job name is the file
              name (without .xml). Use the namespace https://jakarta.ee/xml/ns/jakartaee and version 2.0.
            - Batch artifacts (batchlets, readers, processors, writers, deciders, listeners) are CDI beans, referenced in
              the Job XML by bean name with ref. Annotate them with @Named and @Dependent.
            - Batch properties are injected with @Inject @BatchProperty, and set in the Job XML with
              #{jobParameters['name']} expressions.
            - Job parameters can be configured with quarkus.jberet.job."<job-name>".params.<param>=<value>, and a job
              scheduled with quarkus.jberet.job."<job-name>".cron=<cron>.
            - Typed io.quarkiverse.jberet.runtime.api.ItemReader<T>, ItemProcessor<T, R> and ItemWriter<T> interfaces
              are available, in addition to the jakarta.batch.api.chunk ones.
            """;

    private static final String EXPLAIN_FAILURE = """
            The execution {{executionId}} of the batch job {{jobName}} ended with batch status {{batchStatus}} and exit
            status {{exitStatus}}. The job parameters were: {{parameters}}

            {{jobDefinition}}

            The batch artifacts used by the job are:
            {{artifacts}}

            The step executions are:
            {{steps}}

            Explain why the execution failed, pointing to the step and the artifact that caused the failure, and suggest
            how to fix it, with code if that helps. If the execution did not fail, explain what happened.
            Reply in markdown format in the markdown field. Be concise.
            """;

    private static final String GENERATE_JOB = """
            Create a batch job for the following description: {{description}}

            The application already has the jobs: {{jobs}}
            The application has the following batch artifacts available (ref, types and class). Reuse them when they fit,
            especially the built-in ones:
            {{artifacts}}

            Reply with:
            - jobName: the job name, in kebab case, not one of the existing jobs.
            - jobXml: the complete Job XML.
            - java: the Java source code of any new batch artifacts required by the Job XML, or empty if none is
              required. Do not include artifacts that are already available.
            - configuration: the application.properties configuration for the job (like parameters), or empty if none is
              required.
            - markdown: a short explanation of the job, in markdown format.
            """;

    public record SchedulesInfo(boolean schedulerStarted, List<ScheduleInfo> schedules) {
    }

    public record ScheduleInfo(
            String id,
            String jobName,
            String cron,
            long initialDelayMinutes,
            long intervalMinutes,
            Map<String, String> parameters,
            boolean paused,
            Long nextFireTime,
            Long previousFireTime,
            List<Long> executionIds) {
    }

    public record ActionResult(boolean success, String message, Long executionId) {
        static ActionResult success(String message, long executionId) {
            return new ActionResult(true, message, executionId);
        }

        static ActionResult success(String message) {
            return new ActionResult(true, message, null);
        }

        static ActionResult failure(String message) {
            return new ActionResult(false, message, null);
        }
    }
}
