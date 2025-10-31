package io.quarkiverse.jberet.components.item.jdbc;

import static io.quarkiverse.jberet.components.item.jdbc.JdbcUtils.closeQuietly;
import static io.quarkiverse.jberet.components.item.jdbc.JdbcUtils.getDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import javax.sql.DataSource;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.spi.ArtifactFactory;

@Named("jdbcItemWriter")
public class JdbcBatchItemWriter<T> extends AbstractItemWriter {
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

    public JdbcBatchItemWriter(DataSource dataSource, String sql, ParameterSetter<T> parameterSetter) {
        this.dataSource = dataSource;
        this.sql = sql;
        this.parameterSetter = parameterSetter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeItems(List<Object> items) throws Exception {
        Connection connection = dataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        for (Object item : items) {
            parameterSetter.setValues(preparedStatement, (T) item);
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();

        closeQuietly(preparedStatement);
        closeQuietly(connection);
    }
}
