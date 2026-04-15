package io.quarkiverse.jberet.components.runtime.item.file;

/**
 * The {@link LineMapper} maps a {@link String} line read from a flat file to a result object of type {@code T}. Each
 * invocation of {@link LineMapper#mapLine(String, int)} maps a single line of the file.
 *
 * @param <T> the result type
 * @see FlatFileItemReader
 */
@FunctionalInterface
public interface LineMapper<T> {
    /**
     * Maps a single line of a flat file into a result object of type {@code T}.
     *
     * @param line the line to map
     * @param lineNumber the number of the line in the file (starting at 1)
     * @return the result object of type {@code T}, or {@code null}
     * @throws Exception if an error occurs while mapping the line
     */
    T mapLine(String line, int lineNumber) throws Exception;
}
