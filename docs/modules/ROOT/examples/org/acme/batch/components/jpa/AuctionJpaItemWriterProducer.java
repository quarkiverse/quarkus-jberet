package org.acme.batch.components.jpa;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import io.quarkiverse.jberet.components.runtime.item.jpa.JpaItemWriter;

@Singleton
public class AuctionJpaItemWriterProducer {
    @Inject
    EntityManager entityManager;

    @Produces
    @Dependent
    @Named("auctionsItemWriter")
    public JpaItemWriter<AuctionStatistics> auctionsItemWriter() {
        return new JpaItemWriter<>(entityManager);
    }
}
