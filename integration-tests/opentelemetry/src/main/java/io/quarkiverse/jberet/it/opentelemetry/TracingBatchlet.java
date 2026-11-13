package io.quarkiverse.jberet.it.opentelemetry;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.runtime.BatchStatus;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class TracingBatchlet extends AbstractBatchlet {
    @Inject
    @BatchProperty
    boolean fail;

    @Override
    public String process() {
        if (fail) {
            throw new IllegalStateException("Batchlet failed");
        }
        return BatchStatus.COMPLETED.toString();
    }
}
