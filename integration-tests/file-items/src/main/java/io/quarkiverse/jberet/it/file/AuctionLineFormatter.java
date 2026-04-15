package io.quarkiverse.jberet.it.file;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.quarkiverse.jberet.components.runtime.item.file.LineFormatter;

@Singleton
@Named
public class AuctionLineFormatter implements LineFormatter<Auction> {
    @Override
    public String formatLine(Auction auction) {
        return auction.id() + "," + auction.itemId() + "," + auction.bid() + "," + auction.buyout() + ","
                + auction.quantity();
    }
}
