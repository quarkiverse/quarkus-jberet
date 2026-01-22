package io.quarkiverse.jberet.runtime;

import java.util.function.Supplier;

import org.jberet.repository.JobRepository;

public interface JobRepositorySupplier extends Supplier<JobRepository> {
    String getName();
}
