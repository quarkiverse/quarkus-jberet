package io.quarkiverse.jberet.runtime.api;

/**
 * ItemProcessor is used in chunk processing to operate on an input item and produce an output item.
 *
 * @param <T> the read result type
 * @param <R> the type to write
 */
public interface ItemProcessor<T, R> extends jakarta.batch.api.chunk.ItemProcessor {
    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    default Object processItem(Object item) throws Exception {
        return process((T) item);
    }

    /**
     * The processItem method is part of a chunk step. It accepts an input item from an item reader and returns an item
     * that gets passed onto the item writer. Returning null indicates that the item should not be continued
     * to be processed. This effectively enables processItem to filter out unwanted input items.
     *
     * @param item specifies the input item to process.
     * @return output item to write.
     * @throws Exception thrown for any errors
     */
    R process(T item) throws Exception;
}
