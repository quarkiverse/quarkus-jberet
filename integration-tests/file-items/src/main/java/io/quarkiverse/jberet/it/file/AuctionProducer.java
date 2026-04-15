package io.quarkiverse.jberet.it.file;

import java.nio.file.Path;
import java.util.Properties;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.quarkiverse.jberet.components.runtime.item.file.FlatFileItemReader;
import io.quarkiverse.jberet.components.runtime.item.file.FlatFileItemWriter;

@Singleton
public class AuctionProducer {
    @Inject
    AuctionLineMapper lineMapper;
    @Inject
    AuctionLineFormatter lineFormatter;

    @Produces
    @Dependent
    @Named("auctionsFileItemReader")
    FlatFileItemReader<Auction> auctionsItemReader() {
        return new FlatFileItemReader<>("auctions.csv", lineMapper).setLinesToSkip(1);
    }

    @Produces
    @Dependent
    @Named("auctionsFileItemWriter")
    FlatFileItemWriter<Auction> auctionsItemWriter(JobOperator jobOperator, JobContext jobContext) {
        JobExecution jobExecution = jobOperator.getJobExecution(jobContext.getExecutionId());
        Properties jobParameters = jobExecution.getJobParameters();
        String outputFile = jobParameters.getProperty("outputFile");
        return new FlatFileItemWriter<>(Path.of(outputFile), lineFormatter);
    }
}
