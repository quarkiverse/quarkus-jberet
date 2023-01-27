package io.quarkiverse.jberet.runtime;

import java.util.function.Supplier;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JobRepository;

public class JBeretInMemoryJobRepositoryProducer implements Supplier<JobRepository> {

    @Override
    @Produces
    @Singleton
    public JobRepository get() {
        return new InMemoryRepository();
    }

}
