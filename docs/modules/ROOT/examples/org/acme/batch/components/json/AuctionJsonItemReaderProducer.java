package org.acme.batch.components.json;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.quarkiverse.jberet.components.runtime.item.json.JsonItemReader;
import tools.jackson.databind.ObjectMapper;

@Singleton
public class AuctionJsonItemReaderProducer {
    @Inject
    ObjectMapper objectMapper;

    @Produces
    @Dependent
    @Named("auctionsItemReader")
    public JsonItemReader<Auction> auctionsItemReader() {
        return new JsonItemReader<>("auctions.json", Auction.class, objectMapper);
    }
}
