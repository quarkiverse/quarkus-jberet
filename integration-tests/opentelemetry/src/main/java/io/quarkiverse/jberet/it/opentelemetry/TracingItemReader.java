package io.quarkiverse.jberet.it.opentelemetry;

import java.io.Serializable;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class TracingItemReader extends AbstractItemReader {
    @Inject
    @BatchProperty
    int items;

    private int count;

    @Override
    public void open(final Serializable checkpoint) {
        count = checkpoint != null ? (Integer) checkpoint : 0;
    }

    @Override
    public Object readItem() {
        return count < items ? count++ : null;
    }

    @Override
    public Serializable checkpointInfo() {
        return count;
    }
}
