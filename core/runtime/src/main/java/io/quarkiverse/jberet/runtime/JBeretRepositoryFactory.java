package io.quarkiverse.jberet.runtime;

import static io.quarkiverse.jberet.runtime.JBeretConfig.Repository.Type.JDBC;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JdbcRepository;
import org.jberet.repository.JobRepository;

import io.quarkus.agroal.runtime.DataSources;

final class JBeretRepositoryFactory {
    private JBeretRepositoryFactory() {
        throw new UnsupportedOperationException();
    }

    static JobRepository getJobRepository(final JBeretConfig config) {
        if (JDBC.equals(config.repository().type())) {
            final Properties configProperties = new Properties();

            addJdbcProperty(config.repository().jdbc().sqlFileName(), JdbcRepository.SQL_FILE_NAME_KEY, configProperties);
            addJdbcProperty(config.repository().jdbc().ddlFileName(), JdbcRepository.DDL_FILE_NAME_KEY, configProperties);
            addJdbcProperty(config.repository().jdbc().dbTablePrefix(), JdbcRepository.DB_TABLE_PREFIX_KEY, configProperties);
            addJdbcProperty(config.repository().jdbc().dbTableSuffix(), JdbcRepository.DB_TABLE_SUFFIX_KEY, configProperties);
            return new JdbcRepository(DataSources.fromName(config.repository().jdbc().datasource()), configProperties);
        }

        return new InMemoryRepository();
    }

    private static void addJdbcProperty(Optional<String> value, String jberetPropertyName,
            Properties jdbcRepositoryProperties) {
        value.map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .ifPresent(v -> jdbcRepositoryProperties.put(jberetPropertyName, v));
    }
}
