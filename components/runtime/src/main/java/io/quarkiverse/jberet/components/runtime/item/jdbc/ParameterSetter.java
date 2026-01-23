package io.quarkiverse.jberet.components.runtime.item.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * The {@link ParameterSetter} provides a way to set parameter values into a {@link PreparedStatement} from an object
 * of type {@code T}.
 * <p>
 * The values are set into a wrapper type {@link Parameters}, that can only access the {@code set} methods of
 * the underlying {@link PreparedStatement}.
 *
 * @param <T> the object parameter type
 * @see JdbcBatchItemWriter
 */
@FunctionalInterface
public interface ParameterSetter<T> {
    /**
     * Set parameter values on a {@link Parameters}.
     *
     * @param parameters a {@link Parameters} to call {@code set} methods in a {@link PreparedStatement}
     * @param value the object of type {@code T} with the values to set
     * @throws SQLException if a {@link SQLException} exception is thrown when accessing the {@link PreparedStatement}
     */
    void setValues(final Parameters parameters, final T value) throws SQLException;

    /**
     * Set parameter values on a {@link PreparedStatement}.
     *
     * @param preparedStatement a {@link java.sql.PreparedStatement} to call {@code set} methods
     * @param value the object of type {@code T} with the values to set
     * @throws SQLException if a {@link SQLException} exception is thrown when accessing the {@link PreparedStatement}
     */
    default void setValues(final PreparedStatement preparedStatement, final T value) throws SQLException {
        setValues(new Parameters(preparedStatement), value);
    }

    /**
     * A wrapper type around {@link java.sql.PreparedStatement} to call {@code set} methods. Remaining methods throw
     * an {@link java.lang.UnsupportedOperationException}.
     */
    class Parameters implements PreparedStatement {
        private final PreparedStatement ps;

        public Parameters(final PreparedStatement ps) {
            this.ps = ps;
        }

        // Supported parameter-setting methods - delegate to PreparedStatement
        @Override
        public void setNull(int parameterIndex, int sqlType) throws SQLException {
            ps.setNull(parameterIndex, sqlType);
        }

        @Override
        public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
            ps.setNull(parameterIndex, sqlType, typeName);
        }

        @Override
        public void setBoolean(int parameterIndex, boolean x) throws SQLException {
            ps.setBoolean(parameterIndex, x);
        }

        @Override
        public void setByte(int parameterIndex, byte x) throws SQLException {
            ps.setByte(parameterIndex, x);
        }

        @Override
        public void setShort(int parameterIndex, short x) throws SQLException {
            ps.setShort(parameterIndex, x);
        }

        @Override
        public void setInt(int parameterIndex, int x) throws SQLException {
            ps.setInt(parameterIndex, x);
        }

        @Override
        public void setLong(int parameterIndex, long x) throws SQLException {
            ps.setLong(parameterIndex, x);
        }

        @Override
        public void setFloat(int parameterIndex, float x) throws SQLException {
            ps.setFloat(parameterIndex, x);
        }

        @Override
        public void setDouble(int parameterIndex, double x) throws SQLException {
            ps.setDouble(parameterIndex, x);
        }

        @Override
        public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
            ps.setBigDecimal(parameterIndex, x);
        }

        @Override
        public void setString(int parameterIndex, String x) throws SQLException {
            ps.setString(parameterIndex, x);
        }

        @Override
        public void setBytes(int parameterIndex, byte[] x) throws SQLException {
            ps.setBytes(parameterIndex, x);
        }

        @Override
        public void setDate(int parameterIndex, Date x) throws SQLException {
            ps.setDate(parameterIndex, x);
        }

        @Override
        public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
            ps.setDate(parameterIndex, x, cal);
        }

        @Override
        public void setTime(int parameterIndex, Time x) throws SQLException {
            ps.setTime(parameterIndex, x);
        }

        @Override
        public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
            ps.setTime(parameterIndex, x, cal);
        }

        @Override
        public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
            ps.setTimestamp(parameterIndex, x);
        }

        @Override
        public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
            ps.setTimestamp(parameterIndex, x, cal);
        }

        @Override
        public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
            ps.setAsciiStream(parameterIndex, x, length);
        }

        @Override
        public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
            ps.setAsciiStream(parameterIndex, x, length);
        }

        @Override
        public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
            ps.setAsciiStream(parameterIndex, x);
        }

        @Override
        @Deprecated
        public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
            ps.setUnicodeStream(parameterIndex, x, length);
        }

        @Override
        public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
            ps.setBinaryStream(parameterIndex, x, length);
        }

        @Override
        public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
            ps.setBinaryStream(parameterIndex, x, length);
        }

        @Override
        public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
            ps.setBinaryStream(parameterIndex, x);
        }

        @Override
        public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
            ps.setObject(parameterIndex, x, targetSqlType);
        }

        @Override
        public void setObject(int parameterIndex, Object x) throws SQLException {
            ps.setObject(parameterIndex, x);
        }

        @Override
        public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
            ps.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
        }

        @Override
        public void setObject(int parameterIndex, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
            ps.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
        }

        @Override
        public void setObject(int parameterIndex, Object x, SQLType targetSqlType) throws SQLException {
            ps.setObject(parameterIndex, x, targetSqlType);
        }

        @Override
        public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
            ps.setCharacterStream(parameterIndex, reader, length);
        }

        @Override
        public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
            ps.setCharacterStream(parameterIndex, reader, length);
        }

        @Override
        public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
            ps.setCharacterStream(parameterIndex, reader);
        }

        @Override
        public void setRef(int parameterIndex, Ref x) throws SQLException {
            ps.setRef(parameterIndex, x);
        }

        @Override
        public void setBlob(int parameterIndex, Blob x) throws SQLException {
            ps.setBlob(parameterIndex, x);
        }

        @Override
        public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
            ps.setBlob(parameterIndex, inputStream, length);
        }

        @Override
        public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
            ps.setBlob(parameterIndex, inputStream);
        }

        @Override
        public void setClob(int parameterIndex, Clob x) throws SQLException {
            ps.setClob(parameterIndex, x);
        }

        @Override
        public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
            ps.setClob(parameterIndex, reader, length);
        }

        @Override
        public void setClob(int parameterIndex, Reader reader) throws SQLException {
            ps.setClob(parameterIndex, reader);
        }

        @Override
        public void setArray(int parameterIndex, Array x) throws SQLException {
            ps.setArray(parameterIndex, x);
        }

        @Override
        public void setURL(int parameterIndex, URL x) throws SQLException {
            ps.setURL(parameterIndex, x);
        }

        @Override
        public void setRowId(int parameterIndex, RowId x) throws SQLException {
            ps.setRowId(parameterIndex, x);
        }

        @Override
        public void setNString(int parameterIndex, String value) throws SQLException {
            ps.setNString(parameterIndex, value);
        }

        @Override
        public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
            ps.setNCharacterStream(parameterIndex, value, length);
        }

        @Override
        public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
            ps.setNCharacterStream(parameterIndex, value);
        }

        @Override
        public void setNClob(int parameterIndex, NClob value) throws SQLException {
            ps.setNClob(parameterIndex, value);
        }

        @Override
        public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
            ps.setNClob(parameterIndex, reader, length);
        }

        @Override
        public void setNClob(int parameterIndex, Reader reader) throws SQLException {
            ps.setNClob(parameterIndex, reader);
        }

        @Override
        public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
            ps.setSQLXML(parameterIndex, xmlObject);
        }

        @Override
        public ResultSet executeQuery() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int executeUpdate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearParameters() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean execute() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addBatch() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResultSetMetaData getMetaData() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ParameterMetaData getParameterMetaData() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long executeLargeUpdate() {
            throw new UnsupportedOperationException();
        }

        // Unsupported Statement methods
        @Override
        public ResultSet executeQuery(String sql) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int executeUpdate(String sql) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getMaxFieldSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMaxFieldSize(int max) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getMaxRows() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMaxRows(int max) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setEscapeProcessing(boolean enable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getQueryTimeout() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setQueryTimeout(int seconds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cancel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SQLWarning getWarnings() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearWarnings() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setCursorName(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean execute(String sql) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResultSet getResultSet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getUpdateCount() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getMoreResults() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFetchDirection(int direction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getFetchDirection() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFetchSize(int rows) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getFetchSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getResultSetConcurrency() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getResultSetType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addBatch(String sql) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearBatch() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int[] executeBatch() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Connection getConnection() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getMoreResults(int current) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResultSet getGeneratedKeys() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int executeUpdate(String sql, int autoGeneratedKeys) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int executeUpdate(String sql, int[] columnIndexes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int executeUpdate(String sql, String[] columnNames) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean execute(String sql, int autoGeneratedKeys) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean execute(String sql, int[] columnIndexes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean execute(String sql, String[] columnNames) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getResultSetHoldability() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isClosed() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPoolable(boolean poolable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isPoolable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void closeOnCompletion() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isCloseOnCompletion() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getLargeUpdateCount() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setLargeMaxRows(long max) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getLargeMaxRows() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long[] executeLargeBatch() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long executeLargeUpdate(String sql) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long executeLargeUpdate(String sql, int autoGeneratedKeys) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long executeLargeUpdate(String sql, int[] columnIndexes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long executeLargeUpdate(String sql, String[] columnNames) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            throw new UnsupportedOperationException();
        }
    }
}
