package io.quarkiverse.jberet.it.jpa;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import io.quarkiverse.jberet.components.runtime.item.jpa.JpaItemReader;
import io.quarkiverse.jberet.components.runtime.item.jpa.JpaItemWriter;

@Singleton
public class AuctionProducer {
    @Inject
    EntityManager entityManager;

    @Produces
    @Dependent
    @Named("auctionsItemReader")
    JpaItemReader<Auction> auctionsItemReader() {
        return new JpaItemReader<>(entityManager, "SELECT a FROM Auction a ORDER BY a.id", Auction.class);
    }

    @Produces
    @Dependent
    @Named("auctionsItemWriter")
    JpaItemWriter<AuctionStatistics> auctionsItemWriter() {
        return new JpaItemWriter<>(entityManager);
    }
}
