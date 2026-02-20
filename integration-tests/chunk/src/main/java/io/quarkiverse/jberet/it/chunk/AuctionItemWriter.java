package io.quarkiverse.jberet.it.chunk;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkiverse.jberet.runtime.api.ItemWriter;

@Dependent
@Named
public class AuctionItemWriter implements ItemWriter<Auction> {
    @Inject
    AuctionDatabase database;

    @Override
    public void write(List<Auction> items) throws Exception {
        items.stream().forEach(database::put);
    }
}
