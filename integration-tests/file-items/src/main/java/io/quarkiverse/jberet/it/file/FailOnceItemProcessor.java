package io.quarkiverse.jberet.it.file;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@Named
public class FailOnceItemProcessor implements ItemProcessor {
    static final long FAIL_ON_ID = 279595850L;

    private final AtomicBoolean failed = new AtomicBoolean();

    @Override
    public Object processItem(Object item) {
        Auction auction = (Auction) item;
        if (auction.id() == FAIL_ON_ID && failed.compareAndSet(false, true)) {
            throw new RuntimeException("Failing on id " + FAIL_ON_ID);
        }
        return auction;
    }
}
