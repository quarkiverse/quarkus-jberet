package io.quarkiverse.jberet.runtime;

import static io.quarkiverse.jberet.runtime.JBeretConfig.Repository.Type.JDBC;

import java.util.Properties;

import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JdbcRepository;
import org.jberet.repository.JobRepository;

import io.quarkus.agroal.runtime.DataSources;

final class JBeretRepositoryFactory {
    private JBeretRepositoryFactory() {
        throw new UnsupportedOperationException();
    }

    static JobRepository getJobRepository(final JBeretConfig config) {
        if (JDBC.equals(config.repository.type)) {
            return new JdbcRepository(DataSources.fromName(config.repository.jdbc.datasource), new Properties());
        }

        return new InMemoryRepository();
    }
}
