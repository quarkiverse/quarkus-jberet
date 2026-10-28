package io.quarkiverse.jberet.components.runtime.item.json;

import java.io.Serializable;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

import jakarta.batch.api.BatchProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkiverse.jberet.runtime.api.ItemWriter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

/**
 * Writes data to a JSON file as a JSON array, serializing each item of type {@code T} into an array element using
 * Jackson. Each element is written in its own line.
 * <p>
 * The {@link JsonItemWriter} can be referenced in the Job XML definition by the name <code>jsonItemWriter</code>. It
 * supports the following properties:
 * <ul>
 * <li>{@code resource} (required), a <code>String</code> with the file path to write</li>
 * <li>{@code encoding} (optional), a <code>String</code> with the file encoding. Defaults to UTF-8</li>
 * </ul>
 * <p>
 * When referenced in the Job XML definition, the {@link JsonItemWriter} uses the application {@link ObjectMapper},
 * including any customization applied via Quarkus configuration or {@code JsonMapperBuilderCustomizer}.
 * <p>
 * On restart, the file is truncated to the position of the last checkpoint, discarding any items written by a
 * chunk that did not complete, and the writer continues appending to the existing JSON array.
 * <p>
 * It is also possible to use the {@link JsonItemWriter} programmatically via
 * {@link #JsonItemWriter(Path, ObjectMapper)}
 *
 * @param <T> the type to write
 */
@Named("jsonItemWriter")
public class JsonItemWriter<T> implements ItemWriter<T> {
    private static final String ARRAY_START = "[";
    private static final String ARRAY_END = "]";
    private static final String ELEMENT_SEPARATOR = ",";
    private static final String LINE_SEPARATOR = "\n";

    private final Path resource;
    private final ObjectWriter objectWriter;

    @BatchProperty
    String encoding;

    private FileChannel channel;
    private Writer writer;
    private long arrayStartPosition;
    private long position;

    @Inject
    JsonItemWriter(
            @BatchProperty(name = "resource") String resource,
            ObjectMapper objectMapper) {
        this.resource = Path.of(Objects.requireNonNull(resource, "Batch property resource is required"));
        this.objectWriter = objectMapper.writer();
    }

    /**
     * Constructs a new {@link JsonItemWriter}.
     *
     * @param resource a {@link Path} to the file to write
     * @param objectMapper an {@link ObjectMapper} to serialize each item into a JSON element
     */
    public JsonItemWriter(final Path resource, final ObjectMapper objectMapper) {
        this.resource = resource;
        this.objectWriter = objectMapper.writer();
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        channel = FileChannel.open(resource, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        writer = Channels.newWriter(channel, charset);
        arrayStartPosition = ARRAY_START.getBytes(charset).length;

        if (checkpoint != null) {
            position = (Long) checkpoint;
            if (channel.size() < position) {
                long size = channel.size();
                channel.close();
                channel = null;
                writer = null;
                throw new IllegalStateException("Cannot restart writing to " + resource + ": the file size " + size
                        + " is smaller than the checkpoint position " + position);
            }
            rewind();
        } else {
            channel.truncate(0);
            writer.write(ARRAY_START);
            writer.flush();
            position = channel.position();
        }
    }

    @Override
    public void close() throws Exception {
        try {
            if (writer != null) {
                // discard anything written after the last successful write
                rewind();
                if (position > arrayStartPosition) {
                    writer.write(LINE_SEPARATOR);
                }
                writer.write(ARRAY_END);
                writer.write(LINE_SEPARATOR);
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

        // serialize the whole chunk first, so a failing item does not leave partial content in the file
        StringBuilder chunk = new StringBuilder();
        boolean first = position <= arrayStartPosition;
        for (T item : items) {
            if (!first) {
                chunk.append(ELEMENT_SEPARATOR);
            }
            chunk.append(LINE_SEPARATOR);
            chunk.append(objectWriter.writeValueAsString(item));
            first = false;
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

    private void rewind() throws Exception {
        writer.flush();
        channel.truncate(position);
        channel.position(position);
    }

    @Override
    public Serializable checkpointInfo() {
        return position;
    }

    /**
     * Sets the file encoding. Defaults to UTF-8.
     *
     * @param encoding the encoding to use when writing the file
     * @return this {@link JsonItemWriter}
     */
    public JsonItemWriter<T> setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }
}
