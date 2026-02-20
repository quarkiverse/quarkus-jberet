package io.quarkiverse.jberet.components.runtime.item.jdbc;

import static io.quarkiverse.jberet.components.runtime.item.jdbc.JdbcUtils.closeQuietly;
import static io.quarkiverse.jberet.components.runtime.item.jdbc.JdbcUtils.getDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import javax.sql.DataSource;

import jakarta.batch.api.BatchProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.spi.ArtifactFactory;

import io.quarkiverse.jberet.runtime.api.ItemWriter;

/**
 * Writes data to a {@link DataSource} using JDBC batch processing.
 * <p>
 * The writer uses {@link java.sql.PreparedStatement#addBatch()} and {@link java.sql.PreparedStatement#executeBatch()}
 * to write multiple items in a single database operation. Each item of type <code>T</code> is converted to
 * {@link PreparedStatement} parameters using the provided {@link ParameterSetter}.
 * <p>
 * The writer operates within the transaction boundaries defined by the chunk step. The writer commits the
 * transaction after {@link #writeItems(java.util.List)} completes successfully. If an exception occurs during the
 * write operation, the transaction is rolled back automatically.
 * <p>
 * The {@link JdbcBatchItemWriter} can be referenced in the Job XML definition by the name
 * <code>jdbcItemWriter</code>. It supports the following properties:
 * <ul>
 * <li>{@code dataSource} (optional), a <code>String</code> reference to the CDI name of the {@link DataSource}.
 * If not specified, the default (unnamed) datasource is used.</li>
 * <li>{@code sql} (required), a <code>String</code> with the SQL statement to execute for each item. Must be a
 * parameterized statement with <code>?</code> placeholders.</li>
 * <li>{@code parameterSetter} (required), a <code>String</code> reference to the CDI name of the
 * {@link ParameterSetter}</li>
 * </ul>
 * It is also possible to use the {@link JdbcBatchItemWriter} programmatically via
 * {@link #JdbcBatchItemWriter(javax.sql.DataSource, String, ParameterSetter)}
 *
 * @param <T> the type to write
 */
@Named("jdbcItemWriter")
public class JdbcBatchItemWriter<T> implements ItemWriter<T> {
    private final DataSource dataSource;
    private final String sql;
    private final ParameterSetter<T> parameterSetter;

    @Inject
    @SuppressWarnings("unchecked")
    JdbcBatchItemWriter(
            @BatchProperty(name = "dataSource") String dataSource,
            @BatchProperty(name = "sql") String sql,
            @BatchProperty(name = "parameterSetter") String parameterSetter,
            ArtifactFactory artifactFactory) throws Exception {
        this.dataSource = getDataSource(dataSource);
        this.sql = sql;
        this.parameterSetter = (ParameterSetter<T>) artifactFactory.create(parameterSetter, RowMapper.class, null);
    }

    /**
     * Constructs a new {@link JdbcBatchItemWriter}.
     *
     * @param dataSource a {@link DataSource} to write the data
     * @param sql a <code>String</code> with the SQL statement to execute for each item to write
     * @param parameterSetter a {@link ParameterSetter} to map objects into SQL parameters
     */
    public JdbcBatchItemWriter(DataSource dataSource, String sql, ParameterSetter<T> parameterSetter) {
        this.dataSource = dataSource;
        this.sql = sql;
        this.parameterSetter = parameterSetter;
    }

    @Override
    public void write(List<T> items) throws Exception {
        Connection connection = dataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        for (T item : items) {
            parameterSetter.setValues(preparedStatement, item);
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();

        closeQuietly(preparedStatement);
        closeQuietly(connection);
    }
}
