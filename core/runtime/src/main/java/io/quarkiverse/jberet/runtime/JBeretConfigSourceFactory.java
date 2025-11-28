package io.quarkiverse.jberet.runtime;

import static java.util.Collections.singletonList;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkiverse.jberet.runtime.JobProcessor.JobProcessorBuilder;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;

public class JBeretConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        Set<String> properties = new HashSet<>();

        JobProcessor jobProcessor = new JobProcessorBuilder()
                .propertiesHolderConsumer(propertiesHolder -> properties.addAll(
                        propertiesHolder.getProperties().getNameValues().keySet()))
                .build();
        JBeretDataHolder.getData().getJobs().forEach(jobProcessor::processJob);

        return singletonList(new JBeretConfigSource(properties));
    }
}
