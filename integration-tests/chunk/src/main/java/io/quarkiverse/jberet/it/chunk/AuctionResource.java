package io.quarkiverse.jberet.it.chunk;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auctions")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class AuctionResource {
    @Inject
    AuctionDatabase database;

    @GET
    @Path("/{id}")
    public Response getAuction(@PathParam("id") final Long id) {
        return Optional.ofNullable(database.get(id)).map(Response::ok).orElse(Response.status(NOT_FOUND)).build();
    }
}
