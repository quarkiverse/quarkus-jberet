package io.quarkiverse.jberet.components.runtime.util;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

public class JpaUtils {
    public static EntityManager getEntityManager(Instance<EntityManager> entityManager, String persistenceUnit) {
        if (persistenceUnit == null || persistenceUnit.isEmpty()
                || PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME.equals(persistenceUnit)) {
            return entityManager.select(Default.Literal.INSTANCE).get();
        }
        return entityManager.select(new PersistenceUnit.PersistenceUnitLiteral(persistenceUnit)).get();
    }
}
