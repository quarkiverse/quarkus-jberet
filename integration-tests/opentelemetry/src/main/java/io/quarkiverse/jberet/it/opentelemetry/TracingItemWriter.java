package io.quarkiverse.jberet.it.opentelemetry;

import java.util.List;

import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.inject.Named;

@Named
public class TracingItemWriter extends AbstractItemWriter {
    @Override
    public void writeItems(final List<Object> items) {
    }
}
