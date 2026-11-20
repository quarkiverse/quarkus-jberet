package org.acme.batch.composition;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.jberet.job.model.DecisionBuilder;
import org.jberet.job.model.FlowBuilder;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.SplitBuilder;
import org.jberet.job.model.StepBuilder;

@Singleton
public class AuctionsJob {
    @Produces
    @Named
    public Job auctionsJob() {
        return new JobBuilder("auctionsJob")
                .step(new StepBuilder("download") // <1>
                        .batchlet("auctionsDownloadBatchlet")
                        .next("checkAuctions")
                        .build())
                .decision(new DecisionBuilder("checkAuctions", "auctionsDecider") // <2>
                        .property("minimumAuctions", "100")
                        .nextOn("NEW_AUCTIONS").to("process")
                        .endOn("*").exitStatus()
                        .build())
                .split(new SplitBuilder("process") // <3>
                        .flow(new FlowBuilder("statisticsFlow")
                                .step(new StepBuilder("statistics")
                                        .reader("auctionItemReader", new String[] { "region", "#{partitionPlan['region']}" })
                                        .writer("auctionStatisticsWriter")
                                        .partitionMapper("regionPartitionMapper") // <4>
                                        .build())
                                .build())
                        .flow(new FlowBuilder("archiveFlow")
                                .step(new StepBuilder("archive")
                                        .batchlet("auctionsArchiveBatchlet")
                                        .build())
                                .build())
                        .next("report")
                        .build())
                .step(new StepBuilder("report") // <5>
                        .batchlet("auctionsReportBatchlet")
                        .build())
                .build();
    }
}
