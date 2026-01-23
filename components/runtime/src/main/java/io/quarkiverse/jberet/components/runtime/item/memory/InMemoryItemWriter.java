package io.quarkiverse.jberet.components.runtime.item.memory;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;

import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.inject.Named;

@Named
public class InMemoryItemWriter<T> extends AbstractItemWriter {
    private final List<T> items = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public void writeItems(List<Object> items) {
        for (Object item : items) {
            this.items.add((T) item);
        }
    }

    public List<T> getItems() {
        return unmodifiableList(items);
    }
}
