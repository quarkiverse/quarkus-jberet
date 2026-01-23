package io.quarkiverse.jberet.runtime.repository;

import static org.jberet.repository.JdbcRepository.DB_TABLE_PREFIX_KEY;
import static org.jberet.repository.JdbcRepository.DB_TABLE_SUFFIX_KEY;
import static org.jberet.repository.JdbcRepository.DDL_FILE_NAME_KEY;
import static org.jberet.repository.JdbcRepository.SQL_FILE_NAME_KEY;

import java.util.Optional;
import java.util.Properties;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jberet.repository.JdbcRepository;
import org.jberet.repository.JobRepository;

import io.agroal.api.AgroalDataSource;
import io.quarkiverse.jberet.runtime.JBeretConfig;
import io.quarkiverse.jberet.runtime.JBeretRuntimeConfig;
import io.quarkiverse.jberet.runtime.JobRepositorySupplier;
import io.quarkus.agroal.runtime.AgroalDataSourceUtil;

@Singleton
public class JdbcJobRepositorySupplier implements JobRepositorySupplier {
    public final static String TYPE = "jdbc";

    @Inject
    JBeretConfig config;
    @Inject
    JBeretRuntimeConfig runtimeConfig;

    @Override
    public JobRepository get() {
        Properties configProperties = new Properties();
        addJdbcProperty(config.repository().jdbc().sqlFileName(), SQL_FILE_NAME_KEY, configProperties);
        addJdbcProperty(config.repository().jdbc().ddlFileName(), DDL_FILE_NAME_KEY, configProperties);
        addJdbcProperty(runtimeConfig.repository().jdbc().dbTablePrefix(), DB_TABLE_PREFIX_KEY, configProperties);
        addJdbcProperty(runtimeConfig.repository().jdbc().dbTableSuffix(), DB_TABLE_SUFFIX_KEY, configProperties);

        String datasource = config.repository().jdbc().datasource();
        Optional<AgroalDataSource> dataSource = AgroalDataSourceUtil.dataSourceIfActive(datasource);
        if (dataSource.isEmpty()) {
            throw new IllegalArgumentException("The configured datasource " + datasource + " could not be found. " +
                    "Available configured datasources: " + AgroalDataSourceUtil.activeDataSourceNames());
        }
        return new JdbcRepository(dataSource.get(), configProperties);
    }

    private void addJdbcProperty(Optional<String> value, String jberetPropertyName,
            Properties jdbcRepositoryProperties) {
        value.ifPresent(v -> jdbcRepositoryProperties.put(jberetPropertyName, v));
    }

    @Override
    public String getName() {
        return TYPE;
    }
}
