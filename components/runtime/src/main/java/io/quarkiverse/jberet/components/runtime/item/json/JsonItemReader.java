package io.quarkiverse.jberet.components.runtime.item.json;

import java.io.BufferedReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import jakarta.batch.api.BatchProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkiverse.jberet.components.runtime.util.ResourceUtils;
import io.quarkiverse.jberet.runtime.api.ItemReader;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

/**
 * Reads data from a JSON file, mapping each element of the root JSON array into an object of type {@code T} using
 * Jackson. The file is streamed element by element, without loading the entire file into memory. JSON
 * <code>null</code> elements are skipped.
 * <p>
 * The {@link JsonItemReader} can be referenced in the Job XML definition by the name <code>jsonItemReader</code>. It
 * supports the following properties:
 * <ul>
 * <li>{@code resource} (required), a <code>String</code> with the resource to read. The resource is resolved in
 * order as a file path, or a classpath resource</li>
 * <li>{@code itemType} (required), a <code>String</code> with the FQN of the type to map each JSON element</li>
 * <li>{@code encoding} (optional), a <code>String</code> with the file encoding. Defaults to UTF-8</li>
 * </ul>
 * <p>
 * When referenced in the Job XML definition, the {@link JsonItemReader} uses the application {@link ObjectMapper},
 * including any customization applied via Quarkus configuration or {@code JsonMapperBuilderCustomizer}.
 * <p>
 * It is also possible to use the {@link JsonItemReader} programmatically via
 * {@link #JsonItemReader(String, Class, ObjectMapper)} or {@link #JsonItemReader(Path, Class, ObjectMapper)}
 *
 * @param <T> the read result type
 */
@Named("jsonItemReader")
public class JsonItemReader<T> implements ItemReader<T> {
    private final String resource;
    private final ObjectReader objectReader;

    @BatchProperty
    String encoding;

    private BufferedReader reader;
    private JsonParser parser;
    private int currentItem;

    /**
     * Constructs a new {@link JsonItemReader}. The resource is resolved in order as a file path, or a classpath
     * resource.
     *
     * @param resource a {@link String} with the resource to read
     * @param itemType a {@link Class} with the type to map each JSON element
     * @param objectMapper an {@link ObjectMapper} to map each JSON element into a result object
     */
    @Inject
    public JsonItemReader(
            @BatchProperty(name = "resource") final String resource,
            @BatchProperty(name = "itemType") final Class<T> itemType,
            final ObjectMapper objectMapper) {
        this.resource = Objects.requireNonNull(resource, "Batch property resource is required");
        Objects.requireNonNull(itemType, "Batch property itemType is required");
        // elements are read one at a time from the root array, so the remaining elements are expected
        this.objectReader = objectMapper.readerFor(itemType).without(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    /**
     * Constructs a new {@link JsonItemReader}.
     *
     * @param resource a {@link Path} to the file to read
     * @param itemType a {@link Class} with the type to map each JSON element
     * @param objectMapper an {@link ObjectMapper} to map each JSON element into a result object
     */
    public JsonItemReader(final Path resource, final Class<T> itemType, final ObjectMapper objectMapper) {
        this(resource.toAbsolutePath().toString(), itemType, objectMapper);
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        reader = ResourceUtils.openReader(resource, charset);
        currentItem = 0;

        try {
            parser = objectReader.createParser(reader);

            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected a JSON array as the root element of " + resource);
            }

            if (checkpoint != null) {
                int checkpointItem = (Integer) checkpoint;
                while (currentItem < checkpointItem) {
                    JsonToken token = parser.nextToken();
                    if (token == null || token == JsonToken.END_ARRAY) {
                        break;
                    }
                    parser.skipChildren();
                    currentItem++;
                }
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
        if (parser != null) {
            parser.close();
            parser = null;
        }
        if (reader != null) {
            reader.close();
            reader = null;
        }
    }

    @Override
    public T read() throws Exception {
        if (parser == null) {
            throw new IllegalStateException();
        }

        JsonToken token = parser.nextToken();
        // a null item signals the end of the data, so null elements are skipped
        while (token == JsonToken.VALUE_NULL) {
            currentItem++;
            token = parser.nextToken();
        }
        if (token == null || token == JsonToken.END_ARRAY) {
            return null;
        }

        T item = objectReader.readValue(parser);
        currentItem++;
        return item;
    }

    @Override
    public Serializable checkpointInfo() {
        return currentItem;
    }

    /**
     * Sets the file encoding. Defaults to UTF-8.
     *
     * @param encoding the encoding to use when reading the file
     * @return this {@link JsonItemReader}
     */
    public JsonItemReader<T> setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }
}
