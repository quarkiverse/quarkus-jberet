package io.quarkiverse.jberet.it.mongo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.mongodb.MongoClientName;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;

/**
 * Only injects the reactive client of {@code horde}, while the {@code mongo-named-reactive-injection} job uses the
 * blocking client of {@code horde} in the {@code mongoItemWriter}.
 */
@ApplicationScoped
public class AuctionReactiveRepository {
    @Inject
    @MongoClientName("horde")
    ReactiveMongoClient mongoClient;
}
