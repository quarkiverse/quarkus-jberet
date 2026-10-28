package io.quarkiverse.jberet.it.file;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.StepBuilder;

@ApplicationScoped
public class JsonJobProducer {
    @Produces
    @Named("json-programmatic")
    public Job job() {
        return new JobBuilder("json-programmatic")
                .step(new StepBuilder("jsonStep")
                        .reader("jsonItemReader",
                                new String[] { "resource", "auctions.json" },
                                new String[] { "itemType", Auction.class.getName() })
                        .writer("jsonItemWriter",
                                new String[] { "resource", "#{jobParameters['outputFile']}" })
                        .itemCount(3)
                        .build())
                .build();
    }
}
