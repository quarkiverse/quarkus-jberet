package org.acme.batch.components.jpa;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import io.quarkiverse.jberet.components.runtime.item.jpa.JpaItemReader;

@Singleton
public class AuctionJpaItemReaderProducer {
    @Inject
    EntityManager entityManager;

    @Produces
    @Dependent
    @Named("auctionsItemReader")
    public JpaItemReader<Auction> auctionsItemReader() {
        return new JpaItemReader<>(entityManager, "SELECT a FROM Auction a ORDER BY a.id", Auction.class);
    }
}
