package org.acme.batch.components.file;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.quarkiverse.jberet.components.runtime.item.file.FlatFileItemReader;

@Singleton
public class AuctionFlatFileItemReaderProducer {
    @Inject
    AuctionLineMapper lineMapper;

    @Produces
    @Dependent
    @Named("auctionsItemReader")
    public FlatFileItemReader<Auction> auctionsItemReader() {
        return new FlatFileItemReader<>("auctions.csv", lineMapper)
                .setLinesToSkip(1);
    }
}
