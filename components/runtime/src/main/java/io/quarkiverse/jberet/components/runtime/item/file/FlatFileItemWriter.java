package io.quarkiverse.jberet.components.runtime.item.file;

import java.io.Serializable;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
 * <li>{@code header} (optional), a <code>String</code> with a header line to write at the beginning of the file. The
 * header is not written when appending to a non-empty file, or on restart</li>
 * </ul>
 * <p>
 * On restart, the file is truncated to the position of the last checkpoint, discarding any lines written by a chunk
 * that did not complete, and the writer continues appending to the existing file.
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
    @BatchProperty
    String header;

    private FileChannel channel;
    private Writer writer;
    private long position;

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
        channel = FileChannel.open(resource, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        writer = Channels.newWriter(channel, charset);

        try {
            if (checkpoint != null) {
                position = (Long) checkpoint;
                if (channel.size() < position) {
                    throw new IllegalStateException("Cannot restart writing to " + resource + ": the file size "
                            + channel.size() + " is smaller than the checkpoint position " + position);
                }
                rewind();
            } else if (append != null && append) {
                position = channel.size();
                channel.position(position);
            } else {
                channel.truncate(0);
                position = 0;
            }

            if (checkpoint == null && header != null && position == 0) {
                writer.write(header);
                writer.write(lineSeparator != null ? lineSeparator : System.lineSeparator());
                writer.flush();
                position = channel.position();
            }
        } catch (Exception e) {
            try {
                close();
            } catch (Exception closeException) {
                e.addSuppressed(closeException);
            }
            throw e;
        }
    }

    @Override
    public void close() throws Exception {
        try {
            if (writer != null) {
                writer.close();
            }
        } finally {
            writer = null;
            if (channel != null) {
                channel.close();
                channel = null;
            }
        }
    }

    @Override
    public void write(List<T> items) throws Exception {
        if (writer == null) {
            throw new IllegalStateException();
        }

        String separator = lineSeparator != null ? lineSeparator : System.lineSeparator();

        // format the whole chunk first, so a failing item does not leave partial content in the file
        StringBuilder chunk = new StringBuilder();
        for (T item : items) {
            chunk.append(lineFormatter.formatLine(item));
            chunk.append(separator);
        }

        try {
            writer.write(chunk.toString());
            writer.flush();
        } catch (Exception e) {
            try {
                rewind();
            } catch (Exception rewindException) {
                e.addSuppressed(rewindException);
            }
            throw e;
        }
        position = channel.position();
    }

    @Override
    public Serializable checkpointInfo() {
        return position;
    }

    private void rewind() throws Exception {
        writer.flush();
        channel.truncate(position);
        channel.position(position);
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

    /**
     * Sets a header line to write at the beginning of the file. The header is not written when appending to a
     * non-empty file, or on restart.
     *
     * @param header the header line, without the line separator
     * @return this {@link FlatFileItemWriter}
     */
    public FlatFileItemWriter<T> setHeader(String header) {
        this.header = header;
        return this;
    }
}
