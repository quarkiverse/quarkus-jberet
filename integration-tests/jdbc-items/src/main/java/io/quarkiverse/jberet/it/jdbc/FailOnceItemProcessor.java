package io.quarkiverse.jberet.it.jdbc;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@Named
public class FailOnceItemProcessor implements ItemProcessor {
    static final int FAIL_ON_ITEM_ID = 81971;

    private final AtomicBoolean failed = new AtomicBoolean();

    @Override
    public Object processItem(Object item) {
        AuctionStatistics statistics = (AuctionStatistics) item;
        if (statistics.itemId() == FAIL_ON_ITEM_ID && failed.compareAndSet(false, true)) {
            throw new RuntimeException("Failing on itemId " + FAIL_ON_ITEM_ID);
        }
        return statistics;
    }
}
