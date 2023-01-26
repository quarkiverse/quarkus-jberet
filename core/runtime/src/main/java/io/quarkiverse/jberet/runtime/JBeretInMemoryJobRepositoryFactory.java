package io.quarkiverse.jberet.runtime;

import jakarta.inject.Singleton;

import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JobRepository;

@Singleton
public class JBeretInMemoryJobRepositoryFactory implements JBeretRepositoryFactory {

    @Override
    public JobRepository apply(JBeretConfig config) {
        return new InMemoryRepository();
    }

}
