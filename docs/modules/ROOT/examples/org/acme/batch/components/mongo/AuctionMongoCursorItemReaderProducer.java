package org.acme.batch.components.mongo;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import io.quarkiverse.jberet.components.runtime.item.mongo.MongoCursorItemReader;

@Singleton
public class AuctionMongoCursorItemReaderProducer {
    @Inject
    MongoClient mongoClient;

    @Produces
    @Dependent
    @Named("auctionsMongoItemReader")
    public MongoCursorItemReader<Auction> auctionsMongoItemReader() {
        return new MongoCursorItemReader<>(mongoClient, "auctions", "auctions", Auction.class)
                .setFilter(Filters.gt("buyout", 40000))
                .setSort(Sorts.ascending("itemId"))
                .setLimit(1000);
    }
}
