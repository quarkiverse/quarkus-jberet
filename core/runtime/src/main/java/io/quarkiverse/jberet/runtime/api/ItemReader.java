package io.quarkiverse.jberet.runtime.api;

import java.io.Serializable;

/**
 * ItemReader defines the batch artifact that reads items for chunk processing.
 *
 * @param <T> the read result type
 */
public interface ItemReader<T> extends jakarta.batch.api.chunk.ItemReader {
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
    default Object readItem() throws Exception {
        return read();
    }

    /**
     * The readItem method returns the next item for chunk processing. It returns null to indicate no more items, which
     * also means the current chunk will be committed and the step will end.
     *
     * @return next item or {@code null}
     * @throws Exception is thrown for any errors
     */
    T read() throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    default Serializable checkpointInfo() throws Exception {
        return null;
    }
}
