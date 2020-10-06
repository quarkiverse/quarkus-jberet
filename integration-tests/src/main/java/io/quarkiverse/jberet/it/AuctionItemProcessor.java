package io.quarkiverse.jberet.it;

import java.time.LocalDate;

import javax.batch.api.chunk.ItemProcessor;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

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
