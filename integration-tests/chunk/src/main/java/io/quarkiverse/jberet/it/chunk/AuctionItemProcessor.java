package io.quarkiverse.jberet.it.chunk;

import java.time.LocalDate;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

import io.quarkiverse.jberet.runtime.api.ItemProcessor;

@Dependent
@Named
public class AuctionItemProcessor implements ItemProcessor<Auction, Auction> {

    @Override
    public Auction process(Auction item) {
        item.setProcessedAt(LocalDate.now());
        return item;
    }
}
