package io.quarkiverse.jberet.deployment;

import org.jberet.job.model.Job;

import io.quarkus.builder.item.MultiBuildItem;

public final class BatchJobBuildItem extends MultiBuildItem {
    private final Job job;
    private final String cron;

    public BatchJobBuildItem(final Job job, final String cron) {
        this.job = job;
        this.cron = cron;
    }

    public Job getJob() {
        return job;
    }

    public String getCron() {
        return cron;
    }
}
