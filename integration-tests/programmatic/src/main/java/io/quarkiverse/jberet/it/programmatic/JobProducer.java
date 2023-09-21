package io.quarkiverse.jberet.it.programmatic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.StepBuilder;

@ApplicationScoped
public class JobProducer {
    @Produces
    @Named("cdi")
    public Job job() {
        return new JobBuilder("programmatic")
                .step(new StepBuilder("programmaticStep")
                        .batchlet("programmaticBatchlet")
                        .build())
                .build();
    }
}
