package io.quarkiverse.jberet.it.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auctions/statistics")
@Produces(MediaType.APPLICATION_JSON)
public class AuctionResource {
    @Inject
    DataSource dataSource;

    @GET
    @Path("/count")
    public Response count() {
        try (Connection connection = dataSource.getConnection(); Statement stat = connection.createStatement()) {
            try (ResultSet count = stat.executeQuery("select count(*) from AuctionStatistics")) {
                count.next();
                return Response.ok(count.getLong(1)).build();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/clear")
    public Response clear() {
        try (Connection connection = dataSource.getConnection(); Statement stat = connection.createStatement()) {
            stat.executeUpdate("truncate table AuctionStatistics");
            return Response.ok().build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
