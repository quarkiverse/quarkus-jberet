package org.acme.batch.components.file;

import java.nio.file.Path;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.quarkiverse.jberet.components.runtime.item.file.FlatFileItemWriter;

@Singleton
public class AuctionFlatFileItemWriterProducer {
    @Inject
    AuctionLineFormatter lineFormatter;

    @Produces
    @Dependent
    @Named("auctionsItemWriter")
    public FlatFileItemWriter<Auction> auctionsItemWriter() {
        return new FlatFileItemWriter<>(Path.of("/tmp/auctions-output.csv"), lineFormatter);
    }
}
