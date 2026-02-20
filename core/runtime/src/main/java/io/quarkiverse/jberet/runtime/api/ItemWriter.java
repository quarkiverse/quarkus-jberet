package io.quarkiverse.jberet.runtime.api;

import java.io.Serializable;
import java.util.List;

/**
 * ItemWriter defines the batch artifact that writes to a list of items for chunk processing.
 *
 * @param <T> the type to write
 */
public interface ItemWriter<T> extends jakarta.batch.api.chunk.ItemWriter {
    /**
     * {@inheritDoc}
     */
    @Override
    default void open(Serializable checkpoint) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default void close() throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    default void writeItems(List<Object> items) throws Exception {
        write((List<T>) items);
    }

    /**
     * The writeItems method writes a list of item for the current chunk.
     *
     * @param items specifies the list of items to write. This may be an empty list (e.g. if all the items have been
     *        filtered out by the ItemProcessor).
     * @throws Exception is thrown for any errors
     */
    void write(List<T> items) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    default Serializable checkpointInfo() throws Exception {
        return null;
    }
}
