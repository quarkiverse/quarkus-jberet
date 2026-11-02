package io.quarkiverse.jberet.it.file;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class FailOnceItemProcessor implements ItemProcessor {
    static final long FAIL_ON_ID = 279595850L;

    // fail once per job instance, so each restart test fails its first execution
    private static final Set<Long> FAILED_INSTANCES = ConcurrentHashMap.newKeySet();

    @Inject
    JobContext jobContext;

    @Override
    public Object processItem(Object item) {
        Auction auction = (Auction) item;
        if (auction.id() == FAIL_ON_ID && FAILED_INSTANCES.add(jobContext.getInstanceId())) {
            throw new RuntimeException("Failing on id " + FAIL_ON_ID);
        }
        return auction;
    }
}
