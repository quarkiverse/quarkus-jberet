package io.quarkiverse.jberet.components.runtime.item.jpa;

import static io.quarkiverse.jberet.components.runtime.util.JpaUtils.getEntityManager;

import java.util.List;

import jakarta.batch.api.BatchProperty;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;

import io.quarkiverse.jberet.runtime.api.ItemWriter;

/**
 * Writes data using JPA.
 * <p>
 * The writer operates within the transaction boundaries defined by the chunk step. Each item is merged into the
 * persistence context using {@link EntityManager#merge(Object)}.
 * <p>
 * The {@link JpaItemWriter} can be referenced in the Job XML definition by the name
 * <code>jpaItemWriter</code>. It supports the following properties:
 * <ul>
 * <li>{@code persistenceUnit} (optional), a <code>String</code> with the persistence unit name.
 * If not specified, the default (unnamed) persistence unit is used.</li>
 * </ul>
 * <p>
 * It is also possible to use the {@link JpaItemWriter} programmatically via
 * {@link #JpaItemWriter(jakarta.persistence.EntityManager)}.
 *
 * @param <T> the type to write
 */
@Named("jpaItemWriter")
public class JpaItemWriter<T> implements ItemWriter<T> {
    private final EntityManager entityManager;

    @Inject
    JpaItemWriter(
            @BatchProperty(name = "persistenceUnit") String persistenceUnit,
            @Any Instance<EntityManager> entityManager) {
        this.entityManager = getEntityManager(entityManager, persistenceUnit);
    }

    /**
     * Constructs a new {@link JpaItemWriter}.
     *
     * @param entityManager an {@link EntityManager} to persist the data
     */
    public JpaItemWriter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void write(List<T> items) {
        for (T item : items) {
            entityManager.merge(item);
        }
    }
}
