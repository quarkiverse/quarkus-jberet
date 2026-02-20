package io.quarkiverse.jberet.components.runtime.item.jdbc;

import static io.quarkiverse.jberet.components.runtime.item.jdbc.JdbcUtils.closeQuietly;
import static io.quarkiverse.jberet.components.runtime.item.jdbc.JdbcUtils.getDataSource;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import jakarta.batch.api.BatchProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.spi.ArtifactFactory;

import io.agroal.pool.ConnectionHandler;
import io.agroal.pool.ConnectionHandler.State;
import io.agroal.pool.wrapper.ConnectionWrapper;
import io.quarkiverse.jberet.runtime.api.ItemReader;

/**
 * Reads data from a {@link DataSource} using a JDBC cursor.
 * <p>
 * A cursor is a stream of the JDBC {@link ResultSet}, meaning that it will read every resulting row from the supplied
 * sql statement. Each row {@link ResultSet} calls the provided {@link RowMapper} to map the read result into
 * {@code <T>}.
 * <p>
 * The {@link JdbcCursorItemReader} can be referenced in the Job XMl definition by the name
 * <code>jdbcItemReader</code>. It supports the following properties:
 * <ul>
 * <li>{@code dataSource} (optional), a <code>String</code> reference to the CDI name of the {@link DataSource}.
 * If not specified, the default (unnamed) datasource is used.</li>
 * <li>{@code sql} (required), a <code>String</code> with the SQL query to execute to retrieve the data</li>
 * <li>{@code rowMapper} (required), a <code>String</code> reference to the CDI name of the {@link RowMapper}</li>
 * <li>{@code connectionAutoCommit}, see {@link #setConnectionAutoCommit(boolean)}</li>
 * <li>{@code fetchSize}, see {@link #setFetchSize(int)}</li>
 * </ul>
 * <p>
 * It is also possible to use the {@link JdbcCursorItemReader} programmatically via
 * {@link #JdbcCursorItemReader(javax.sql.DataSource, String, RowMapper)}
 *
 * @param <T> the read result type
 */
@Named("jdbcItemReader")
public class JdbcCursorItemReader<T> implements ItemReader<T> {
    private final DataSource dataSource;
    private final String sql;
    private final RowMapper<T> rowMapper;

    @BatchProperty
    Boolean connectionAutoCommit;
    @BatchProperty
    Integer fetchSize;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private int currentRow;

    @Inject
    @SuppressWarnings("unchecked")
    JdbcCursorItemReader(
            @BatchProperty(name = "dataSource") String dataSource,
            @BatchProperty(name = "sql") String sql,
            @BatchProperty(name = "rowMapper") String rowMapper,
            ArtifactFactory artifactFactory) throws Exception {
        this.dataSource = getDataSource(dataSource);
        this.sql = sql;
        this.rowMapper = (RowMapper<T>) artifactFactory.create(rowMapper, RowMapper.class, null);
    }

    /**
     * Constructs a new {@link JdbcCursorItemReader}.
     *
     * @param dataSource a {@link DataSource} to read the data
     * @param sql a <code>String</code> with the SQL statement to execute to retrieve the data
     * @param rowMapper a {@link RowMapper} to map the SQL data into a result object
     */
    public JdbcCursorItemReader(final DataSource dataSource, final String sql, final RowMapper<T> rowMapper) {
        this.dataSource = dataSource;
        this.sql = sql;
        this.rowMapper = rowMapper;
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        connection = detachConnection(dataSource.getConnection());
        if (connectionAutoCommit != null) {
            connection.setAutoCommit(connectionAutoCommit);
        }

        preparedStatement = connection.prepareStatement(sql,
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY,
                ResultSet.HOLD_CURSORS_OVER_COMMIT);
        if (fetchSize != null) {
            preparedStatement.setFetchSize(fetchSize);
        }

        resultSet = preparedStatement.executeQuery();

        // TODO - Test Checkpoint
        if (checkpoint != null) {
            resultSet.absolute((Integer) checkpoint);
        }
    }

    @Override
    public void close() {
        closeQuietly(resultSet);
        closeQuietly(preparedStatement);
        closeQuietly(connection);
    }

    @Override
    public T read() throws Exception {
        if (resultSet == null) {
            throw new IllegalStateException();
        }

        if (resultSet.next()) {
            T item = rowMapper.mapRow(resultSet);
            currentRow = resultSet.getRow();
            return item;
        }

        return null;
    }

    @Override
    public Serializable checkpointInfo() {
        return currentRow;
    }

    /**
     * Sets the JDBC {@link java.sql.Connection} auto-commit mode to the given state. If a connection is in auto-commit
     * mode, then all its SQL statements will be executed and committed as individual transactions.
     *
     * @param connectionAutoCommit {@code true} to enable auto-commit mode; {@code false} to disable it
     * @return this {@link JdbcCursorItemReader}
     */
    public JdbcCursorItemReader<T> setConnectionAutoCommit(boolean connectionAutoCommit) {
        this.connectionAutoCommit = connectionAutoCommit;
        return this;
    }

    /**
     * Gives the JDBC driver a hint as to the number of rows that should be fetched from the database when more rows
     * are needed for {@code ResultSet} objects generated by this {@code JdbcCursorItemReader}.
     *
     * @param fetchSize the number of rows to fetch
     * @return this {@link JdbcCursorItemReader}
     */
    public JdbcCursorItemReader<T> setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
        return this;
    }

    /**
     * Retrieves the real JDBC Connection instead of the wrapped Connection returned by Agroal. Agroal wrapped
     * connection automatically closes all Closeable elements associated with the Connection on commit, not honoring
     * ResultSet.HOLD_CURSORS_OVER_COMMIT. This also detach the connection from the pool, so it doesn't get acquired
     * before it is closed manually by the reader.
     */
    private static Connection detachConnection(final Connection connection) {
        ConnectionWrapper connectionWrapper = ((ConnectionWrapper) connection);
        ConnectionHandler handler = connectionWrapper.getHandler();
        handler.setState(State.CHECKED_OUT, State.FLUSH);
        handler.transactionBeforeCompletion(false);
        return handler.detachedWrapper();
    }
}
