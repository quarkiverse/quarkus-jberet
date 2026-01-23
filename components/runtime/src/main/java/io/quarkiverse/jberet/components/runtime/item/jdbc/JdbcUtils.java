package io.quarkiverse.jberet.components.runtime.item.jdbc;

import static io.quarkus.agroal.runtime.AgroalDataSourceUtil.dataSourceIfActive;
import static io.quarkus.datasource.common.runtime.DataSourceUtil.DEFAULT_DATASOURCE_NAME;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import javax.sql.DataSource;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;

public final class JdbcUtils {
    private static final Logger LOG = Logger.getLogger(JdbcUtils.class);

    private JdbcUtils() {
        throw new UnsupportedOperationException();
    }

    public static DataSource getDataSource(final String name) {
        Optional<AgroalDataSource> dataSource = dataSourceIfActive(name == null ? DEFAULT_DATASOURCE_NAME : name);
        if (dataSource.isEmpty()) {
            // TODO - Proper error message
            throw new IllegalArgumentException();
        }
        return dataSource.get();
    }

    public static void closeQuietly(final Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (final SQLException e) {
                LOG.trace("Failed to close JDBC Connection", e);
            }
        }
    }

    public static void closeQuietly(final Statement preparedStatement) {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (final SQLException e) {
                LOG.trace("Failed to close JDBC Statement", e);
            }
        }
    }

    public static void closeQuietly(final ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (final SQLException e) {
                LOG.trace("Failed to close JDBC ResultSet", e);
            }
        }
    }
}
