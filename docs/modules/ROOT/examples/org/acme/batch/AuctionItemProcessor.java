package org.acme.batch;

import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.inject.Named;

@Named
public class AuctionItemProcessor implements ItemProcessor {

    @Override
    public Object processItem(Object item) { // <1>
        Auction auction = (Auction) item;
        if (auction.bid() <= 0 ||
                auction.buyout() <= 0 ||
                auction.quantity() <= 0 ||
                auction.buyout() < auction.bid()) {
            return null;
        }
        return auction;
    }
}
