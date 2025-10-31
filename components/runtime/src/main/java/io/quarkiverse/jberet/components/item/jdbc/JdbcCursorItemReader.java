package io.quarkiverse.jberet.components.item.jdbc;

import static io.quarkiverse.jberet.components.item.jdbc.JdbcUtils.closeQuietly;
import static io.quarkiverse.jberet.components.item.jdbc.JdbcUtils.getDataSource;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.ItemReader;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.spi.ArtifactFactory;

import io.agroal.pool.ConnectionHandler;
import io.agroal.pool.ConnectionHandler.State;
import io.agroal.pool.wrapper.ConnectionWrapper;

@Named("jdbcItemReader")
public class JdbcCursorItemReader<T> implements ItemReader {
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
    public Object readItem() throws Exception {
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

    public JdbcCursorItemReader<T> setConnectionAutoCommit(boolean connectionAutoCommit) {
        this.connectionAutoCommit = connectionAutoCommit;
        return this;
    }

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
