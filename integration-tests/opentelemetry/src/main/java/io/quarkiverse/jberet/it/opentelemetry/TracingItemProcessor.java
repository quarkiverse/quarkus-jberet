package io.quarkiverse.jberet.it.opentelemetry;

import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.inject.Named;

@Named
public class TracingItemProcessor implements ItemProcessor {
    @Override
    public Object processItem(final Object item) {
        return item;
    }
}
