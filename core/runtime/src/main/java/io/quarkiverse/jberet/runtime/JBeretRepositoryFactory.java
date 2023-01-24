package io.quarkiverse.jberet.runtime;

import java.util.function.Function;

import org.jberet.repository.JobRepository;

public interface JBeretRepositoryFactory extends Function<JBeretConfig, JobRepository> {
}
