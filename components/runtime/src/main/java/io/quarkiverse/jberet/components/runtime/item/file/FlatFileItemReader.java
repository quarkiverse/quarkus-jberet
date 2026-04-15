package io.quarkiverse.jberet.components.runtime.item.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.batch.api.BatchProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.spi.ArtifactFactory;

import io.quarkiverse.jberet.runtime.api.ItemReader;
import io.smallrye.common.classloader.ClassPathUtils;

/**
 * Reads data from a flat file, mapping each line into an object of type {@code T} using a {@link LineMapper}.
 * <p>
 * The {@link FlatFileItemReader} can be referenced in the Job XML definition by the name
 * <code>flatFileItemReader</code>. It supports the following properties:
 * <ul>
 * <li>{@code resource} (required), a <code>String</code> with the resource to read. The resource is resolved in
 * order as a URL, a file path, or a classpath resource</li>
 * <li>{@code lineMapper} (required), a <code>String</code> reference to the CDI name of the {@link LineMapper}</li>
 * <li>{@code encoding} (optional), a <code>String</code> with the file encoding. Defaults to UTF-8</li>
 * <li>{@code linesToSkip} (optional), an <code>int</code> with the number of header lines to skip. Defaults to 0</li>
 * </ul>
 * <p>
 * It is also possible to use the {@link FlatFileItemReader} programmatically via
 * {@link #FlatFileItemReader(String, LineMapper)} or {@link #FlatFileItemReader(Path, LineMapper)}
 *
 * @param <T> the read result type
 */
@Named("flatFileItemReader")
public class FlatFileItemReader<T> implements ItemReader<T> {
    private final String resource;
    private final LineMapper<T> lineMapper;

    @BatchProperty
    String encoding;
    @BatchProperty
    Integer linesToSkip;

    private BufferedReader reader;
    private int currentLine;

    @Inject
    @SuppressWarnings("unchecked")
    FlatFileItemReader(
            @BatchProperty(name = "resource") String resource,
            @BatchProperty(name = "lineMapper") String lineMapper,
            ArtifactFactory artifactFactory) throws Exception {
        this.resource = resource;
        this.lineMapper = (LineMapper<T>) artifactFactory.create(lineMapper, LineMapper.class, null);
    }

    /**
     * Constructs a new {@link FlatFileItemReader}. The resource is resolved in order as a URL, a file path, or a
     * classpath resource.
     *
     * @param resource a {@link String} with the resource to read
     * @param lineMapper a {@link LineMapper} to map each line into a result object
     */
    public FlatFileItemReader(final String resource, final LineMapper<T> lineMapper) {
        this.resource = resource;
        this.lineMapper = lineMapper;
    }

    /**
     * Constructs a new {@link FlatFileItemReader}.
     *
     * @param resource a {@link Path} to the file to read
     * @param lineMapper a {@link LineMapper} to map each line into a result object
     */
    public FlatFileItemReader(final Path resource, final LineMapper<T> lineMapper) {
        this.resource = resource.toAbsolutePath().toString();
        this.lineMapper = lineMapper;
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        reader = openResource(charset);
        currentLine = 0;

        if (checkpoint != null) {
            int checkpointLine = (Integer) checkpoint;
            while (currentLine < checkpointLine) {
                reader.readLine();
                currentLine++;
            }
        } else {
            int skip = linesToSkip != null ? linesToSkip : 0;
            for (int i = 0; i < skip; i++) {
                reader.readLine();
                currentLine++;
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (reader != null) {
            reader.close();
        }
    }

    @Override
    public T read() throws Exception {
        if (reader == null) {
            throw new IllegalStateException();
        }

        String line = reader.readLine();
        if (line != null) {
            currentLine++;
            return lineMapper.mapLine(line, currentLine);
        }

        return null;
    }

    @Override
    public Serializable checkpointInfo() {
        return currentLine;
    }

    private BufferedReader openResource(Charset charset) throws IOException {
        // Try as file path
        Path path = Path.of(resource);
        if (Files.exists(path)) {
            return Files.newBufferedReader(path, charset);
        }

        // Try as classpath resource
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = FlatFileItemReader.class.getClassLoader();
        }
        URL url = cl.getResource(resource);
        if (url != null) {
            String protocol = url.getProtocol();
            if ("file".equals(protocol) || "jar".equals(protocol)) {
                return ClassPathUtils.processAsPath(url, p -> {
                    try {
                        return Files.newBufferedReader(p, charset);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
            // Fallback for other protocols
            InputStream is = url.openStream();
            return new BufferedReader(new InputStreamReader(is, charset));
        }

        throw new IllegalArgumentException("Resource not found: " + resource);
    }

    /**
     * Sets the file encoding. Defaults to UTF-8.
     *
     * @param encoding the encoding to use when reading the file
     * @return this {@link FlatFileItemReader}
     */
    public FlatFileItemReader<T> setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    /**
     * Sets the number of header lines to skip at the beginning of the file. Defaults to 0.
     *
     * @param linesToSkip the number of lines to skip
     * @return this {@link FlatFileItemReader}
     */
    public FlatFileItemReader<T> setLinesToSkip(int linesToSkip) {
        this.linesToSkip = linesToSkip;
        return this;
    }
}
