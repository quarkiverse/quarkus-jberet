package io.quarkiverse.jberet.runtime.dev.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import org.jberet.job.model.Listeners;
import org.jberet.job.model.RefArtifact;

import io.quarkiverse.jberet.runtime.JobProcessor;
import io.quarkiverse.jberet.runtime.JobProcessor.JobProcessorBuilder;
import io.quarkiverse.jberet.runtime.QuarkusJobOperator;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

/**
 * Collects the events of Job and Step executions in Dev mode, to show them in the Dev UI. The events are produced by
 * the {@link JBeretDevJobListener} and the {@link JBeretDevStepListener}, which are added to every Job and Step on
 * startup.
 */
@ApplicationScoped
public class JBeretDevEvents {
    private static final int RECENT_EVENTS = 100;

    private final BroadcastProcessor<BatchEvent> events = BroadcastProcessor.create();
    private final Deque<BatchEvent> recentEvents = new ArrayDeque<>(RECENT_EVENTS);
    private final AtomicLong ids = new AtomicLong();

    @Inject
    QuarkusJobOperator jobOperator;

    /**
     * Adds the Dev listeners to the Jobs before anything else can start them.
     */
    void registerListeners(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) StartupEvent event) {
        JobProcessor listeners = new JobProcessorBuilder()
                .jobListenersConsumer(new Consumer<Listeners>() {
                    @Override
                    public void accept(final Listeners listeners) {
                        listeners.getListeners().add(new RefArtifact(JBeretDevJobListener.NAME));
                    }
                })
                .stepListenersConsumer(new Consumer<Listeners>() {
                    @Override
                    public void accept(final Listeners listeners) {
                        listeners.getListeners().add(new RefArtifact(JBeretDevStepListener.NAME));
                    }
                })
                .build();
        jobOperator.getJobs().values().forEach(listeners::processJob);
    }

    Multi<BatchEvent> stream() {
        return events;
    }

    List<BatchEvent> recent() {
        synchronized (recentEvents) {
            return new ArrayList<>(recentEvents);
        }
    }

    void publish(
            final String type,
            final String jobName,
            final long executionId,
            final String stepName,
            final String batchStatus,
            final String exitStatus,
            final Map<String, Long> metrics,
            final String message) {
        BatchEvent event = new BatchEvent(ids.incrementAndGet(), System.currentTimeMillis(), type, jobName, executionId,
                stepName, batchStatus, exitStatus, metrics, message);
        synchronized (recentEvents) {
            if (recentEvents.size() == RECENT_EVENTS) {
                recentEvents.removeFirst();
            }
            recentEvents.addLast(event);
        }
        events.onNext(event);
    }

    /**
     * An event of a Job or Step execution.
     *
     * @param id a sequential id of the event
     * @param timestamp when the event happened, in epoch millis
     * @param type the type of event: <code>JOB_STARTED</code>, <code>JOB_ENDED</code>, <code>STEP_STARTED</code> or
     *        <code>STEP_ENDED</code>
     * @param jobName the Job name (id)
     * @param executionId the Job execution id
     * @param stepName the Step name, for Step events
     * @param batchStatus the batch status
     * @param exitStatus the exit status, for ended events
     * @param metrics the Step metrics, for ended Step events
     * @param message the failure, if the Step failed
     */
    public record BatchEvent(
            long id,
            long timestamp,
            String type,
            String jobName,
            long executionId,
            String stepName,
            String batchStatus,
            String exitStatus,
            Map<String, Long> metrics,
            String message) {
    }
}
