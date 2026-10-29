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
import io.quarkiverse.jberet.components.runtime.item.json.JsonItemReader;
import io.quarkiverse.jberet.components.runtime.item.json.JsonItemWriter;
import tools.jackson.databind.ObjectMapper;

@Singleton
public class AuctionProducer {
    @Inject
    AuctionLineMapper lineMapper;
    @Inject
    AuctionLineFormatter lineFormatter;
    @Inject
    ObjectMapper objectMapper;

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

    @Produces
    @Dependent
    @Named("auctionsJsonItemReader")
    JsonItemReader<Auction> auctionsJsonItemReader() {
        return new JsonItemReader<>("auctions.json", Auction.class, objectMapper);
    }

    @Produces
    @Dependent
    @Named("auctionsJsonItemWriter")
    JsonItemWriter<Auction> auctionsJsonItemWriter(JobOperator jobOperator, JobContext jobContext) {
        JobExecution jobExecution = jobOperator.getJobExecution(jobContext.getExecutionId());
        Properties jobParameters = jobExecution.getJobParameters();
        String outputFile = jobParameters.getProperty("outputFile");
        return new JsonItemWriter<>(Path.of(outputFile), objectMapper);
    }
}
