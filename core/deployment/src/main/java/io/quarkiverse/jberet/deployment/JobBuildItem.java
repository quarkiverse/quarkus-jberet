package io.quarkiverse.jberet.deployment;

import org.jberet.job.model.Job;

import io.quarkus.builder.item.MultiBuildItem;

public final class JobBuildItem extends MultiBuildItem {
    private final Job job;

    public JobBuildItem(final Job job) {
        this.job = job;
    }

    public Job getJob() {
        return job;
    }
}
