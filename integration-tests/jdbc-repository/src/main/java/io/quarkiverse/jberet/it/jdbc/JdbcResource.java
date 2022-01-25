package io.quarkiverse.jberet.it.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.batch.runtime.JobInstance;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jberet.runtime.JobInstanceImpl;

@Path("/jdbc")
@Produces(MediaType.APPLICATION_JSON)
public class JdbcResource {
    @Inject
    @io.quarkus.agroal.DataSource("batch")
    DataSource dataSource;

    @ConfigProperty(name = "job-instance-tablename", defaultValue = "JOB_INSTANCE")
    String jobInstanceTableName;

    @GET
    @Path("/instances/{name}")
    public Response instances(@PathParam("name") final String name) throws Exception {
        final List<JobInstance> jobInstances = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            final String sql = "SELECT * FROM " + jobInstanceTableName + " WHERE JOBNAME=? ORDER BY JOBINSTANCEID DESC";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, name);
                preparedStatement.execute();
                try (ResultSet resultSet = preparedStatement.getResultSet()) {
                    while (resultSet.next()) {
                        final JobInstanceImpl jobInstance = new JobInstanceImpl(null, null, resultSet.getString("JOBNAME"));
                        jobInstance.setId(resultSet.getInt("JOBINSTANCEID"));
                        jobInstances.add(jobInstance);
                    }
                }
            }
        }

        return Response.ok(jobInstances).build();
    }

    @GET
    @Path("/tables/{name}")
    public Response tables(@PathParam("name") final String tableName) throws Exception {
        final int dot = tableName.indexOf('.');
        String schema = null;
        String table = tableName;
        if (dot != -1) {
            schema = tableName.substring(0, dot).toUpperCase();
            table = tableName.substring(dot + 1);
        }
        try (Connection connection = dataSource.getConnection()) {
            try (ResultSet rs = connection.getMetaData().getTables(null, schema, table.toUpperCase(),
                    new String[] { "TABLE" })) {
                if (!rs.next()) {
                    throw new NotFoundException();
                }
                final String ts = rs.getString("TABLE_SCHEM");
                final String tn = rs.getString("TABLE_NAME");
                return Response.ok(ts + '.' + tn).build();
            }
        }
    }
}
