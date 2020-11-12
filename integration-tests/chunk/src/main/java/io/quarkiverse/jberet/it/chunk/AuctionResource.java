package io.quarkiverse.jberet.it.chunk;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
