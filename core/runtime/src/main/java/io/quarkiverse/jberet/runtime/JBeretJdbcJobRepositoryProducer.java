package io.quarkiverse.jberet.runtime;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jberet.repository.JdbcRepository;
import org.jberet.repository.JobRepository;

import io.quarkus.agroal.runtime.DataSources;

public class JBeretJdbcJobRepositoryProducer implements Supplier<JobRepository> {

    public final static String TYPE = "jdbc";

    @Inject
    JBeretConfig config;
    @Inject
    DataSources dataSources;

    @Override
    @Produces
    @Singleton
    public JobRepository get() {
        final Properties configProperties = new Properties();
        addJdbcProperty(config.repository().jdbc().sqlFileName(), JdbcRepository.SQL_FILE_NAME_KEY, configProperties);
        addJdbcProperty(config.repository().jdbc().ddlFileName(), JdbcRepository.DDL_FILE_NAME_KEY, configProperties);
        addJdbcProperty(config.repository().jdbc().dbTablePrefix(), JdbcRepository.DB_TABLE_PREFIX_KEY, configProperties);
        addJdbcProperty(config.repository().jdbc().dbTableSuffix(), JdbcRepository.DB_TABLE_SUFFIX_KEY, configProperties);
        return new JdbcRepository(dataSources.getDataSource(config.repository().jdbc().datasource()), configProperties);
    }

    private void addJdbcProperty(Optional<String> value, String jberetPropertyName,
            Properties jdbcRepositoryProperties) {
        value.map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .ifPresent(v -> jdbcRepositoryProperties.put(jberetPropertyName, v));
    }
}
