package io.quarkiverse.jberet.runtime;

import java.util.function.Consumer;

import org.jberet.job.model.Decision;
import org.jberet.job.model.Flow;
import org.jberet.job.model.InheritableJobElement;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Split;
import org.jberet.job.model.Step;

class JobProcessor {
    private final Consumer<Job> jobConsumer;
    private final Consumer<Step> stepConsumer;

    private JobProcessor(Consumer<Job> jobConsumer, Consumer<Step> stepConsumer) {
        this.jobConsumer = jobConsumer;
        this.stepConsumer = stepConsumer;
    }

    void processJob(Job job) {
        jobConsumer.accept(job);

        if (job.getListeners() != null) {
            for (RefArtifact listener : job.getListeners().getListeners()) {
                // process job listener
            }
        }

        for (JobElement jobElement : job.getJobElements()) {
            processJob(jobElement);
        }

        for (InheritableJobElement inheritingJobElement : job.getInheritingJobElements()) {
            processJob(inheritingJobElement);
        }
    }

    private void processJob(JobElement jobElement) {
        if (jobElement instanceof Step step) {
            stepConsumer.accept(step);

            // step.getBatchlet() -> Consume Batchlet

            if (step.getListeners() != null) {
                for (RefArtifact listener : step.getListeners().getListeners()) {
                    // process step listener
                }
            }

            if (step.getChunk() != null) {
                // process chunk reader
                // process chunk processor
                // process chunk writer
                // process chuck checkpoint algorithm
            }

            if (step.getPartition() != null) {
                // process partition mapper
                // process partition collector
                // process partition analyzer
                // process partition reducer
            }
        }

        if (jobElement instanceof Flow flow) {
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
            // process decision
        }
    }

    static class JobProcessorBuilder {
        Consumer<Job> jobConsumer = job -> {
        };
        Consumer<Step> stepConsumer = step -> {
        };

        JobProcessorBuilder jobConsumer(Consumer<Job> jobConsumer) {
            this.jobConsumer = jobConsumer;
            return this;
        }

        JobProcessorBuilder stepConsumer(final Consumer<Step> stepConsumer) {
            this.stepConsumer = stepConsumer;
            return this;
        }

        JobProcessor build() {
            return new JobProcessor(jobConsumer, stepConsumer);
        }
    }
}
