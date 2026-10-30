package io.quarkiverse.jberet.it.mongo.repository;

import java.util.concurrent.CountDownLatch;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Dependent
@Named
public class StoppableBatchlet extends AbstractBatchlet {
    private final CountDownLatch stopped = new CountDownLatch(1);

    @Override
    public String process() throws Exception {
        stopped.await();
        return BatchStatus.STOPPED.toString();
    }

    @Override
    public void stop() {
        stopped.countDown();
    }
}
