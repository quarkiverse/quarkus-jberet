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
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

@Singleton
@Transactional
public class JBeretJpaJobRepository extends AbstractDelegatingJobRepository<JpaRepository> {

    public final static String TYPE = "JPA";

    @Inject
    protected JBeretJpaJobRepositoryConfig config;

    @Any
    @Inject
    protected Instance<EntityManager> entityManager;

    @Override
    public JpaRepository get() {
        return new JpaRepository(
                PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME.equals(config.repository().jpa().persistenceUnitName())
                        ? entityManager.select()
                                .get()
                        : entityManager.select(
                                new PersistenceUnit.PersistenceUnitLiteral(
                                        config.repository().jpa().persistenceUnitName()))
                                .get());
    }

}
