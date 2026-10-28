package io.quarkiverse.jberet.it.chunk;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

import io.quarkiverse.jberet.runtime.api.ItemProcessor;

@Dependent
@Named
public class FailingAuctionItemProcessor implements ItemProcessor<Auction, Auction> {
    @Override
    public Auction process(Auction item) {
        throw new AuctionProcessingException("Failed to process auction " + item.getId(),
                new IllegalStateException("Invalid auction"));
    }
}
