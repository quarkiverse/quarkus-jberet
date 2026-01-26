package io.quarkiverse.jberet.components.runtime.repository;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import org.jberet.jpa.repository.JpaRepository;

import io.quarkiverse.jberet.components.runtime.repository.JobRepositoryConfig.Repository.Jpa;
import io.quarkiverse.jberet.runtime.JobRepositorySupplier;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

@Singleton
public class JpaJobRepositorySupplier implements JobRepositorySupplier {
    @Inject
    protected JobRepositoryConfig config;
    @Any
    @Inject
    protected Instance<EntityManager> entityManager;

    @Override
    public JpaRepository get() {
        String persistenceUnitName = config.repository().jpa().persistenceUnitName();
        return new JpaRepository(
                PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME.equals(persistenceUnitName)
                        ? entityManager.select().get()
                        : entityManager.select(new PersistenceUnit.PersistenceUnitLiteral(persistenceUnitName)).get());
    }

    @Override
    public String getName() {
        return Jpa.TYPE;
    }
}
