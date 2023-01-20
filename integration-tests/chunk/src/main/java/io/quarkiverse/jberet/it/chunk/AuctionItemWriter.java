package io.quarkiverse.jberet.it.chunk;

import java.util.List;

import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Dependent
@Named
public class AuctionItemWriter extends AbstractItemWriter {
    @Inject
    AuctionDatabase database;

    @Override
    public void writeItems(List<Object> items) {
        items.stream().map(Auction.class::cast).forEach(database::put);
    }
}
