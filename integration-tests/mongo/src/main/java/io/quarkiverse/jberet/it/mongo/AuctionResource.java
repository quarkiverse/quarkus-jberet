package io.quarkiverse.jberet.it.mongo;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

import io.quarkiverse.jberet.components.runtime.item.memory.InMemoryItemWriter;
import io.quarkiverse.jberet.components.runtime.item.mongo.MongoClients;

@Path("/auctions")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class AuctionResource {
    @Inject
    @Any
    Instance<MongoClient> mongoClients;

    @GET
    @Path("/default/{id}")
    public Response getAuction(@PathParam("id") final Long id) {
        return getAuction(null, id);
    }

    @GET
    @Path("/{name}/{id}")
    public Response getAuction(@PathParam("name") final String name, @PathParam("id") final Long id) {
        MongoClient mongoClient = MongoClients.getMongoClient(name);
        MongoCollection<Auction> collection = mongoClient.getDatabase("auctions").getCollection("auctions", Auction.class);
        FindIterable<Auction> auctions = collection.find();
        for (Auction auction : auctions) {
            if (auction.getId().equals(id)) {
                return Response.ok(auction).build();
            }
        }
        return Response.status(NOT_FOUND).build();
    }

    @Inject
    @Named("auctionsInMemoryItemWriter")
    InMemoryItemWriter<Auction> auctionsInMemoryItemWriter;

    @GET
    @Path("/beans")
    public Response getAuctions() {
        return Response.ok(auctionsInMemoryItemWriter.getItems()).build();
    }

    @GET
    @Path("/{name}/")
    public Response getAuctions(@PathParam("name") final String name) {
        MongoClient mongoClient = MongoClients.getMongoClient(name);
        MongoCollection<Auction> collection = mongoClient.getDatabase("auctions").getCollection("auctions", Auction.class);
        List<Auction> auctions = new ArrayList<>();
        collection.find().iterator().forEachRemaining(auctions::add);
        return Response.ok(auctions).build();
    }
}
