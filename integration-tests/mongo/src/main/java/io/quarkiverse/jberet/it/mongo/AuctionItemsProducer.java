package io.quarkiverse.jberet.it.mongo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;

import io.quarkiverse.jberet.components.runtime.item.memory.InMemoryItemWriter;
import io.quarkiverse.jberet.components.runtime.item.mongo.MongoCursorItemReader;
import io.quarkiverse.jberet.components.runtime.item.mongo.MongoItemWriter;
import io.quarkus.mongodb.MongoClientName;

@Singleton
public class AuctionItemsProducer {
    @Inject
    @MongoClientName("beans")
    MongoClient mongoClient;

    @Produces
    @Dependent
    @Named("auctionsMongoItemReader")
    MongoCursorItemReader<Auction> auctionsMongoItemReader() {
        return new MongoCursorItemReader<>(mongoClient, "beans", "beans", Auction.class)
                .setFilter(Filters.gt("buyout", 40000))
                .setSkip(3)
                .setLimit(2);
    }

    @Produces
    @Dependent
    @Named("auctionsMongoItemWriter")
    MongoItemWriter<Auction> auctionsMongoItemWriter() {
        return new MongoItemWriter<>(mongoClient, "beans", "beans", Auction.class);
    }

    @Produces
    @ApplicationScoped
    @Named("auctionsInMemoryItemWriter")
    InMemoryItemWriter<Auction> auctionsInMemoryItemWriter() {
        return new InMemoryItemWriter<>();
    }
}
