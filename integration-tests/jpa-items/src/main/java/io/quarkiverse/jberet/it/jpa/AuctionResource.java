package io.quarkiverse.jberet.it.jpa;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auctions/statistics")
@Produces(MediaType.APPLICATION_JSON)
public class AuctionResource {
    @Inject
    EntityManager entityManager;

    @GET
    @Path("/count")
    public Response count() {
        Long count = entityManager.createQuery("SELECT COUNT(a) FROM AuctionStatistics a", Long.class).getSingleResult();
        return Response.ok(count).build();
    }

    @GET
    @Path("/clear")
    @Transactional
    public Response clear() {
        entityManager.createQuery("DELETE FROM AuctionStatistics").executeUpdate();
        return Response.ok().build();
    }
}
