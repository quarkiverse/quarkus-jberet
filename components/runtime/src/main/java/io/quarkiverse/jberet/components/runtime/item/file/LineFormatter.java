package io.quarkiverse.jberet.components.runtime.item.file;

/**
 * The {@link LineFormatter} formats an object of type {@code T} into a {@link String} line to be written to a flat
 * file.
 *
 * @param <T> the object type to format
 * @see FlatFileItemWriter
 */
@FunctionalInterface
public interface LineFormatter<T> {
    /**
     * Formats an object of type {@code T} into a {@link String} line.
     *
     * @param item the object to format
     * @return a {@link String} representation of the object
     * @throws Exception if an error occurs while formatting the item
     */
    String formatLine(T item) throws Exception;
}
