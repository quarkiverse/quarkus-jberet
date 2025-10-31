package io.quarkiverse.jberet.it.jdbc;

public record AuctionStatistics(
        Integer itemId,
        Long quantity,
        Long bid,
        Long minBid,
        Long maxBid,
        Long buyout,
        Long minBuyout,
        Long maxBuyout,
        Double avgBid,
        Double avgBuyout) {
}