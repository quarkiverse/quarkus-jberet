package io.quarkiverse.jberet.jpa.job.repository;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.jberet.jpa.repository.JpaRepository;

import io.quarkiverse.jberet.runtime.AbstractDelegatingJobRepository;
import io.quarkus.hibernate.orm.PersistenceUnit;

@Singleton
@Transactional
public class JBeretJpaJobRepository extends AbstractDelegatingJobRepository<JpaRepository> {

    @Inject
    protected JBeretJpaJobRepositoryConfig config;

    @Any
    @Inject
    protected Instance<EntityManager> entityManager;

    @Override
    public JpaRepository get() {
        return new JpaRepository(
                entityManager.select(
                        new PersistenceUnit.PersistenceUnitLiteral(
                                config.repository().jpa().persistenceUnitName()))
                        .get());
    }

}
