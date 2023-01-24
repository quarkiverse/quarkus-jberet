package io.quarkiverse.jberet.runtime;

import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JobRepository;

public class JBeretInMemoryRepositoryFactory implements JBeretRepositoryFactory {

    static public final String NAME = "IN_MEMORY";

    @Override
    public JobRepository apply(JBeretConfig config) {
        return new InMemoryRepository();
    }

}
