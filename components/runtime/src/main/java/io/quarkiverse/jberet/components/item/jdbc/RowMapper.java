package io.quarkiverse.jberet.components.item.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The {@link RowMapper} maps a {@link ResultSet} to a result object of type {@code T}. Each invocation of
 * {@link RowMapper#mapRow(ResultSet)} maps a single row of the {@link ResultSet}.
 *
 * @param <T> the result type
 * @see JdbcCursorItemReader
 */
@FunctionalInterface
public interface RowMapper<T> {
    /**
     * Maps a single row of {@link java.sql.ResultSet} into a result object of type {@code T}.
     *
     * @param resultSet the {@link ResultSet} to map
     * @return the result object of type {@code T}, or {@code null}
     * @throws SQLException if a {@link SQLException} exception is thrown when accessing the {@link ResultSet}
     */
    T mapRow(final ResultSet resultSet) throws SQLException;
}
