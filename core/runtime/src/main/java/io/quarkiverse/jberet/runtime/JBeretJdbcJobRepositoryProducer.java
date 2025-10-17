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

import io.agroal.api.AgroalDataSource;
import io.quarkiverse.jberet.runtime.JBeretConfig.Repository.Jdbc;
import io.quarkus.agroal.runtime.AgroalDataSourceUtil;

public class JBeretJdbcJobRepositoryProducer implements Supplier<JobRepository> {
    public final static String TYPE = "jdbc";

    @Inject
    JBeretConfig config;

    @Override
    @Produces
    @Singleton
    public JobRepository get() {
        Properties configProperties = new Properties();
        Jdbc jdbc = config.repository().jdbc();
        addJdbcProperty(jdbc.sqlFileName(), JdbcRepository.SQL_FILE_NAME_KEY, configProperties);
        addJdbcProperty(jdbc.ddlFileName(), JdbcRepository.DDL_FILE_NAME_KEY, configProperties);
        addJdbcProperty(jdbc.dbTablePrefix(), JdbcRepository.DB_TABLE_PREFIX_KEY, configProperties);
        addJdbcProperty(jdbc.dbTableSuffix(), JdbcRepository.DB_TABLE_SUFFIX_KEY, configProperties);

        Optional<AgroalDataSource> dataSource = AgroalDataSourceUtil.dataSourceIfActive(jdbc.datasource());
        if (dataSource.isEmpty()) {
            throw new IllegalStateException("No configured datasource " + jdbc.datasource() + " could be found.");
        }
        return new JdbcRepository(dataSource.get(), configProperties);
    }

    private void addJdbcProperty(Optional<String> value, String jberetPropertyName,
            Properties jdbcRepositoryProperties) {
        value.map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .ifPresent(v -> jdbcRepositoryProperties.put(jberetPropertyName, v));
    }
}
