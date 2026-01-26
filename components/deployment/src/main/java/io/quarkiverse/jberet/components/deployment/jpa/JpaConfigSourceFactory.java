package io.quarkiverse.jberet.components.deployment.jpa;

import static io.quarkiverse.jberet.components.runtime.repository.JobRepositoryConfig.DEFAULT_PERSISTENCE_UNIT_NAME;
import static java.lang.Integer.MIN_VALUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkiverse.jberet.components.runtime.repository.JobRepositoryConfig;
import io.quarkiverse.jberet.components.runtime.repository.JobRepositoryConfig.Repository.Jpa;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

/**
 * Generate configuration to register JBeret JPA entities with the configured Persistence Unit. See
 * <a href="https://quarkus.io/guides/hibernate-orm#multiple-persistence-units">Hibernate Persistence Units</a>. This
 * is required in build time by the Hibernate Extension, so the configuration must be generated before build steps
 * execute.
 */
public class JpaConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        ConfigValue repositoryType = context.getValue("quarkus.jberet.repository.type");
        if (!Jpa.TYPE.equalsIgnoreCase(repositoryType.getValue())) {
            return emptyList();
        }

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                .withMapping(JobRepositoryConfig.class)
                .withMappingIgnore("quarkus.**")
                .build();

        JobRepositoryConfig jobRepositoryConfig = config.getConfigMapping(JobRepositoryConfig.class);

        Map<String, String> properties = new HashMap<>();
        String persistenceUnitName = jobRepositoryConfig.repository().jpa().persistenceUnitName();
        String packagesKey = persistenceUnitName.equals(DEFAULT_PERSISTENCE_UNIT_NAME)
                ? "quarkus.hibernate-orm.packages"
                : "quarkus.hibernate-orm.\"" + persistenceUnitName + "\".packages";

        properties.put(packagesKey, "org.jberet.jpa.repository.entity");

        return singletonList(new MapBackedConfigSource("JBeret JPA Config", properties, MIN_VALUE + 10000) {
        });
    }
}
