package io.quarkiverse.jberet.components.runtime.repository;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.jberet")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface JobRepositoryConfig {
    String DEFAULT_PERSISTENCE_UNIT_NAME = "<default>";

    /**
     * The JBeret Repository configuration.
     */
    Repository repository();

    interface Repository {
        /**
         * The JBeret Repository JPA configuration.
         */
        Jpa jpa();

        interface Jpa {
            String TYPE = "jpa";

            /**
             * The Persistence Unit Name for JBeret entities. By default, it uses the default Persistence Unit Name
             * from the Hibernate ORM Extension.
             */
            @WithName("persistence-unit-name")
            @WithDefault(DEFAULT_PERSISTENCE_UNIT_NAME)
            @ConfigDocDefault(value = DEFAULT_PERSISTENCE_UNIT_NAME, escape = false)
            String persistenceUnitName();
        }
    }
}
