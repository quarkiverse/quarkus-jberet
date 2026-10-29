package io.quarkiverse.jberet.components.runtime.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ResourceUtils {
    private ResourceUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Opens a resource for reading. The resource is resolved in order as a file path, or a classpath resource.
     *
     * @param resource the resource to open
     * @param charset the {@link Charset} to decode the resource
     * @return a {@link BufferedReader} to read the resource
     * @throws IOException if an error occurs while opening the resource
     * @throws IllegalArgumentException if the resource is not found
     */
    public static BufferedReader openReader(final String resource, final Charset charset) throws IOException {
        // Try as file path
        Path path = Path.of(resource);
        if (Files.exists(path)) {
            return Files.newBufferedReader(path, charset);
        }

        // Try as classpath resource
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ResourceUtils.class.getClassLoader();
        }
        URL url = cl.getResource(resource);
        if (url != null) {
            // Use the URL stream, since a jar FileSystem would be closed before the reader is consumed
            return new BufferedReader(new InputStreamReader(url.openStream(), charset));
        }

        throw new IllegalArgumentException("Resource not found: " + resource);
    }
}
