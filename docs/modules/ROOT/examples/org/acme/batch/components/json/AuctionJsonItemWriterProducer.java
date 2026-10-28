package org.acme.batch.components.json;

import java.nio.file.Path;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.quarkiverse.jberet.components.runtime.item.json.JsonItemWriter;
import tools.jackson.databind.ObjectMapper;

@Singleton
public class AuctionJsonItemWriterProducer {
    @Inject
    ObjectMapper objectMapper;

    @Produces
    @Dependent
    @Named("auctionsItemWriter")
    public JsonItemWriter<Auction> auctionsItemWriter() {
        return new JsonItemWriter<>(Path.of("/tmp/auctions-output.json"), objectMapper);
    }
}
