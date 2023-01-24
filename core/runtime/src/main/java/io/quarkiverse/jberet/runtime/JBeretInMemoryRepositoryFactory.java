package io.quarkiverse.jberet.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JobRepository;

@Named("IN_MEMORY")
@ApplicationScoped
public class JBeretInMemoryRepositoryFactory implements JBeretRepositoryFactory {

    @Override
    public JobRepository apply(JBeretConfig config) {
        return new InMemoryRepository();
    }

}
