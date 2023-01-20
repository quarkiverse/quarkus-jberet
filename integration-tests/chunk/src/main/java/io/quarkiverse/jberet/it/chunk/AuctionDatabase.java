package io.quarkiverse.jberet.it.chunk;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuctionDatabase {
    private final ConcurrentMap<Long, Auction> database = new ConcurrentHashMap<>();

    void put(Auction auction) {
        database.put(auction.getId(), auction);
    }

    Auction get(Long id) {
        return database.get(id);
    }

    public boolean isEmpty() {
        return database.isEmpty();
    }
}
