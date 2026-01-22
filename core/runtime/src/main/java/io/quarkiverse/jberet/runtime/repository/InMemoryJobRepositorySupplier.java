package io.quarkiverse.jberet.runtime.repository;

import jakarta.inject.Singleton;

import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JobRepository;

import io.quarkiverse.jberet.runtime.JobRepositorySupplier;

@Singleton
public class InMemoryJobRepositorySupplier implements JobRepositorySupplier {
    public final static String TYPE = "in-memory";

    @Override
    public JobRepository get() {
        return new InMemoryRepository();
    }

    @Override
    public String getName() {
        return TYPE;
    }
}
