package io.quarkiverse.jberet.it.jpa;

import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@Named("auctionStatisticsProcessor")
public class AuctionProcessor implements ItemProcessor {
    @Override
    public Object processItem(Object item) {
        Auction auction = (Auction) item;
        AuctionStatistics statistics = new AuctionStatistics();
        statistics.setItemId(auction.getItemId());
        statistics.setQuantity((long) auction.getQuantity());
        statistics.setBid(auction.getBid());
        statistics.setMinBid(auction.getBid() / auction.getQuantity());
        statistics.setMaxBid(auction.getBid() / auction.getQuantity());
        statistics.setBuyout(auction.getBuyout());
        statistics.setMinBuyout(auction.getBuyout() / auction.getQuantity());
        statistics.setMaxBuyout(auction.getBuyout() / auction.getQuantity());
        statistics.setAvgBid((double) (auction.getBid() / auction.getQuantity()));
        statistics.setAvgBuyout((double) (auction.getBuyout() / auction.getQuantity()));
        statistics.setTimestamp(System.currentTimeMillis());
        return statistics;
    }
}
