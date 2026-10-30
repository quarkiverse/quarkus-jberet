package io.quarkiverse.jberet.it.mongo.repository;

import java.io.Serializable;

import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
@Named
public class CountingItemReader extends AbstractItemReader {
    static final int ITEMS = 6;

    @ConfigProperty(name = "mongo.reader.fail.at", defaultValue = "-1")
    Integer failAt;

    private int index;

    @Override
    public void open(final Serializable checkpoint) {
        index = checkpoint != null ? (Integer) checkpoint : 0;
    }

    @Override
    public Object readItem() {
        if (index == failAt) {
            throw new IllegalStateException("Failed at " + index);
        }
        return index < ITEMS ? index++ : null;
    }

    @Override
    public Serializable checkpointInfo() {
        return index;
    }
}
