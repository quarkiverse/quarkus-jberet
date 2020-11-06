package io.quarkiverse.jberet.runtime;

import java.util.Collections;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class JBeretConfigSourceProvider implements ConfigSourceProvider {
    public final Set<String> properties;

    public JBeretConfigSourceProvider(final Set<String> properties) {
        this.properties = Collections.unmodifiableSet(properties);
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(final ClassLoader forClassLoader) {
        return Collections.singletonList(new JBeretConfigSource(properties));
    }
}
