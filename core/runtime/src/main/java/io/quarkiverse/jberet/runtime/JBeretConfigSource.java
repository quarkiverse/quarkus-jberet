package io.quarkiverse.jberet.runtime;

import static org.jberet.creation.ArtifactCreationContext.getCurrentArtifactCreationContext;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jberet.job.model.Properties;

public class JBeretConfigSource implements ConfigSource {
    public final Set<String> properties;

    public JBeretConfigSource(final Set<String> properties) {
        this.properties = properties;
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public String getValue(final String propertyName) {
        if (properties.contains(propertyName)) {
            final Properties properties = getCurrentArtifactCreationContext().getProperties();
            if (properties != null) {
                return properties.get(propertyName);
            }
        }

        return null;
    }

    @Override
    public String getName() {
        return JBeretConfigSource.class.getName();
    }

    @Override
    public int getOrdinal() {
        return ConfigSource.DEFAULT_ORDINAL + 270;
    }
}
