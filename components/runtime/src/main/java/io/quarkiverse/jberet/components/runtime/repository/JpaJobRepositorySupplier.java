package io.quarkiverse.jberet.components.runtime.repository;

import static io.quarkiverse.jberet.components.runtime.util.JpaUtils.getEntityManager;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import org.jberet.jpa.repository.JpaRepository;

import io.quarkiverse.jberet.components.runtime.repository.JobRepositoryConfig.Repository.Jpa;
import io.quarkiverse.jberet.runtime.JobRepositorySupplier;

@Singleton
public class JpaJobRepositorySupplier implements JobRepositorySupplier {
    @Inject
    protected JobRepositoryConfig config;
    @Any
    @Inject
    protected Instance<EntityManager> entityManager;

    @Override
    public JpaRepository get() {
        return new JpaRepository(getEntityManager(entityManager, config.repository().jpa().persistenceUnitName()));
    }

    @Override
    public String getName() {
        return Jpa.TYPE;
    }
}
