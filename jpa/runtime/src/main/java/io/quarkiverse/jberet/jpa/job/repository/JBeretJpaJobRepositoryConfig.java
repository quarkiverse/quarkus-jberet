package io.quarkiverse.jberet.jpa.job.repository;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

import io.quarkiverse.jberet.runtime.JBeretConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = JBeretConfig.PREFIX)
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface JBeretJpaJobRepositoryConfig {

    /**
     *
     */
    Repository repository();

    interface Repository {

        /**
         *
         */
        Jpa jpa();

        interface Jpa {
            /**
             * Persistence Unit Name used for JBeret entities
             */
            @WithName("persistence-unit-name")
            @WithDefault(DEFAULT_PERSISTENCE_UNIT_NAME)
            String persistenceUnitName();
        }
    }
}
