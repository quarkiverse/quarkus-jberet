package io.quarkiverse.jberet.jpa.job.repository.deployment;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkiverse.jberet.jpa.job.repository.JpaJobRepositoryConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.runtime.configuration.CharsetConverter;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

public class JBeretJpaConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                .withConverter(Charset.class, 100, new CharsetConverter())
                .withMapping(JpaJobRepositoryConfig.class)
                .withMapping(HibernateOrmConfig.class)
                .withMappingIgnore("quarkus.**")
                .build();

        JpaJobRepositoryConfig jpaJobRepositoryConfig = config.getConfigMapping(JpaJobRepositoryConfig.class);
        HibernateOrmConfig hibernateOrmConfig = config.getConfigMapping(HibernateOrmConfig.class);

        Map<String, String> properties = new HashMap<>();
        // Find the persistence unit for the JBeret model
        String jberetJpaPersistenceUnitName = jpaJobRepositoryConfig.repository().jpa().persistenceUnitName();
        // Find if there are packaged and append the JBeret model
        HibernateOrmConfigPersistenceUnit persistenceUnit = hibernateOrmConfig.persistenceUnits()
                .get(jberetJpaPersistenceUnitName);
        if (persistenceUnit != null) {
            Set<String> packages = new HashSet<>(persistenceUnit.packages().orElse(new HashSet<>()));
            packages.add("org.jberet.jpa.repository.entity");

            String packagesKey = jberetJpaPersistenceUnitName.equals(DEFAULT_PERSISTENCE_UNIT_NAME)
                    ? "quarkus.hibernate-orm.packages"
                    : "quarkus.hibernate-orm.\"" + jberetJpaPersistenceUnitName + "\".packages";

            properties.put(packagesKey, String.join(",", packages));

            return Collections.singletonList(new MapBackedConfigSource("JBeret JPA Config", properties, Integer.MIN_VALUE) {
            });
        }

        return Collections.emptyList();
    }
}
