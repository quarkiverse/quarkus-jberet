package org.acme.batch.components.mongo;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.mongodb.client.MongoClient;

import io.quarkiverse.jberet.components.runtime.item.mongo.MongoItemWriter;

@Singleton
public class AuctionMongoItemWriterProducer {
    @Inject
    MongoClient mongoClient;

    @Produces
    @Dependent
    @Named("auctionsMongoItemWriter")
    public MongoItemWriter<Auction> auctionsMongoItemWriter() {
        return new MongoItemWriter<>(mongoClient, "auctions", "auctions", Auction.class);
    }
}
