package io.quarkiverse.jberet.it.mongo.repository;

import java.util.List;

import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Dependent
@Named
public class NoopItemWriter extends AbstractItemWriter {
    @Override
    public void writeItems(final List<Object> items) {
    }
}
