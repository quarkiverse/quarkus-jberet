package io.quarkiverse.jberet.components.runtime.item.jpa;

import static io.quarkiverse.jberet.components.runtime.util.JpaUtils.getEntityManager;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import jakarta.batch.api.BatchProperty;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import io.quarkiverse.jberet.runtime.api.ItemReader;

/**
 * Reads data using JPA queries with automatic pagination.
 * <p>
 * Since JPA does not support database cursors natively, the reader pages through results by issuing repeated queries
 * with {@link TypedQuery#setFirstResult(int)} and {@link TypedQuery#setMaxResults(int)}. The {@code pageSize}
 * property controls how many rows are fetched per page (default {@value #DEFAULT_PAGE_SIZE}).
 * <p>
 * The {@link JpaItemReader} can be referenced in the Job XML definition by the name
 * <code>jpaItemReader</code>. It supports the following properties:
 * <ul>
 * <li>{@code query} (required), a <code>String</code> with the JPQL query to execute</li>
 * <li>{@code entityType} (required), a <code>String</code> with the FQN of the entity class</li>
 * <li>{@code persistenceUnit} (optional), a <code>String</code> with the persistence unit name.
 * If not specified, the default (unnamed) persistence unit is used.</li>
 * <li>{@code pageSize}, see {@link #setPageSize(int)}</li>
 * </ul>
 * <p>
 * It is also possible to use the {@link JpaItemReader} programmatically via
 * {@link #JpaItemReader(jakarta.persistence.EntityManager, String, Class)}.
 *
 * @param <T> the read result type
 */
@Named("jpaItemReader")
public class JpaItemReader<T> implements ItemReader<T> {
    static final int DEFAULT_PAGE_SIZE = 10;

    private final EntityManager entityManager;
    private final String query;
    private final Class<T> entityType;

    @BatchProperty
    Integer pageSize;

    private int currentOffset;
    private Iterator<T> currentPage;
    private boolean exhausted;

    @Inject
    JpaItemReader(
            @BatchProperty(name = "persistenceUnit") String persistenceUnit,
            @BatchProperty(name = "query") String query,
            @BatchProperty(name = "entityType") Class<T> entityType,
            @Any Instance<EntityManager> entityManager) {
        this.entityManager = getEntityManager(entityManager, persistenceUnit);
        this.query = query;
        this.entityType = entityType;
    }

    /**
     * Constructs a new {@link JpaItemReader}.
     *
     * @param entityManager an {@link EntityManager} to query the data
     * @param query a <code>String</code> with the JPQL query to execute
     * @param entityType a <code>Class</code> with the entity type of the query results
     */
    public JpaItemReader(final EntityManager entityManager, final String query, final Class<T> entityType) {
        this.entityManager = entityManager;
        this.query = query;
        this.entityType = entityType;
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        if (checkpoint != null) {
            currentOffset = (int) checkpoint;
        }
    }

    @Override
    public T read() throws Exception {
        if (exhausted) {
            return null;
        }

        if (currentPage == null || !currentPage.hasNext()) {
            currentPage = nextPage();
        }

        if (!currentPage.hasNext()) {
            exhausted = true;
            return null;
        }

        T item = currentPage.next();
        currentOffset++;
        return item;
    }

    @Override
    public Serializable checkpointInfo() {
        return currentOffset;
    }

    private Iterator<T> nextPage() {
        int effectivePageSize = pageSize != null ? pageSize : DEFAULT_PAGE_SIZE;
        TypedQuery<T> typedQuery = entityManager.createQuery(query, entityType);
        typedQuery.setFirstResult(currentOffset);
        typedQuery.setMaxResults(effectivePageSize);

        List<T> results = typedQuery.getResultList();
        if (results.size() < effectivePageSize) {
            exhausted = results.isEmpty();
        }
        return results.iterator();
    }

    /**
     * Sets the number of results to fetch per page. Defaults to {@value #DEFAULT_PAGE_SIZE}.
     *
     * @param pageSize the number of results per page
     * @return this {@link JpaItemReader}
     */
    public JpaItemReader<T> setPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }
}
