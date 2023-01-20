package io.quarkiverse.jberet.it.chunk;

import java.time.LocalDate;

import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Dependent
@Named
public class AuctionItemProcessor implements ItemProcessor {
    @Override
    public Object processItem(Object item) {
        Auction auction = (Auction) item;

        auction.setProcessedAt(LocalDate.now());

        return auction;
    }
}
