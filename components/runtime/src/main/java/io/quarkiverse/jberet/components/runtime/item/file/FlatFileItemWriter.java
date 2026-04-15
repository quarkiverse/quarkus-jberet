package io.quarkiverse.jberet.components.runtime.item.file;

import java.io.BufferedWriter;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import jakarta.batch.api.BatchProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.spi.ArtifactFactory;

import io.quarkiverse.jberet.runtime.api.ItemWriter;

/**
 * Writes data to a flat file, formatting each item of type {@code T} into a line using a {@link LineFormatter}.
 * <p>
 * The {@link FlatFileItemWriter} can be referenced in the Job XML definition by the name
 * <code>flatFileItemWriter</code>. It supports the following properties:
 * <ul>
 * <li>{@code resource} (required), a <code>String</code> with the file path to write</li>
 * <li>{@code lineFormatter} (required), a <code>String</code> reference to the CDI name of the
 * {@link LineFormatter}</li>
 * <li>{@code encoding} (optional), a <code>String</code> with the file encoding. Defaults to UTF-8</li>
 * <li>{@code lineSeparator} (optional), a <code>String</code> with the line separator. Defaults to the system line
 * separator</li>
 * <li>{@code append} (optional), a <code>boolean</code> indicating whether to append to the file. Defaults to
 * false</li>
 * </ul>
 * <p>
 * It is also possible to use the {@link FlatFileItemWriter} programmatically via
 * {@link #FlatFileItemWriter(Path, LineFormatter)}
 *
 * @param <T> the type to write
 */
@Named("flatFileItemWriter")
public class FlatFileItemWriter<T> implements ItemWriter<T> {
    private final Path resource;
    private final LineFormatter<T> lineFormatter;

    @BatchProperty
    String encoding;
    @BatchProperty
    String lineSeparator;
    @BatchProperty
    Boolean append;

    private BufferedWriter writer;

    @Inject
    @SuppressWarnings("unchecked")
    FlatFileItemWriter(
            @BatchProperty(name = "resource") String resource,
            @BatchProperty(name = "lineFormatter") String lineFormatter,
            ArtifactFactory artifactFactory) throws Exception {
        this.resource = Path.of(resource);
        this.lineFormatter = (LineFormatter<T>) artifactFactory.create(lineFormatter, LineFormatter.class, null);
    }

    /**
     * Constructs a new {@link FlatFileItemWriter}.
     *
     * @param resource a {@link Path} to the file to write
     * @param lineFormatter a {@link LineFormatter} to format each item into a line
     */
    public FlatFileItemWriter(final Path resource, final LineFormatter<T> lineFormatter) {
        this.resource = resource;
        this.lineFormatter = lineFormatter;
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;

        if (checkpoint != null) {
            writer = Files.newBufferedWriter(resource, charset, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } else {
            boolean shouldAppend = append != null ? append : false;
            if (shouldAppend) {
                writer = Files.newBufferedWriter(resource, charset, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                writer = Files.newBufferedWriter(resource, charset, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (writer != null) {
            writer.close();
        }
    }

    @Override
    public void write(List<T> items) throws Exception {
        if (writer == null) {
            throw new IllegalStateException();
        }

        String separator = lineSeparator != null ? lineSeparator : System.lineSeparator();

        for (T item : items) {
            writer.write(lineFormatter.formatLine(item));
            writer.write(separator);
        }
        writer.flush();
    }

    /**
     * Sets the file encoding. Defaults to UTF-8.
     *
     * @param encoding the encoding to use when writing the file
     * @return this {@link FlatFileItemWriter}
     */
    public FlatFileItemWriter<T> setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    /**
     * Sets the line separator. Defaults to the system line separator.
     *
     * @param lineSeparator the line separator to use
     * @return this {@link FlatFileItemWriter}
     */
    public FlatFileItemWriter<T> setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
        return this;
    }

    /**
     * Sets whether to append to the file instead of overwriting it. Defaults to false.
     *
     * @param append {@code true} to append to the file; {@code false} to overwrite
     * @return this {@link FlatFileItemWriter}
     */
    public FlatFileItemWriter<T> setAppend(boolean append) {
        this.append = append;
        return this;
    }
}
