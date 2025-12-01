package org.acme.batch;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.StepBuilder;

@Singleton
public class FileCleanupJob {
    @Produces
    @Named
    public Job fileCleanupJob() {
        return new JobBuilder("fileCleanupJob")
                .step(new StepBuilder("cleanupStep")
                        .batchlet("fileCleanupBatchlet")
                        .property("directory", "/tmp/batch")
                        .property("daysToKepp", "30")
                        .build())
                .build();
    }
}
