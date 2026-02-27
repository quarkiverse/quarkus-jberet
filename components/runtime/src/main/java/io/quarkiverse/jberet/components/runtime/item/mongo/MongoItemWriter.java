package io.quarkiverse.jberet.components.runtime.item.mongo;

import java.util.List;

import jakarta.batch.api.BatchProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.TransactionalException;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;

import io.quarkiverse.jberet.runtime.api.ItemWriter;
import io.quarkus.arc.Arc;

/**
 * Writes data to a MongoDB collection.
 * <p>
 * The writer operates within the transaction boundaries defined by the chunk step. The writer commits the
 * transaction after {@link #writeItems(java.util.List)} completes successfully. If an exception occurs during the
 * write operation, the transaction is rolled back automatically.
 * <p>
 * The {@link MongoItemWriter} can be referenced in the Job XMl definition by the name
 * <code>mongoItemWriter</code>. It supports the following properties:
 * <ul>
 * <li>{@code name} (optional), a {@code String} reference to the CDI name of the {@link MongoClient}.
 * If not specified, the default (unnamed) MongoClient is used.</li>
 * <li>{@code database} (required), a {@code String} with the MongoDB database to use.</li>
 * <li>{@code collection} (required), a {@code String} with the MongoDB collection to use.</li>
 * <li>{@code itemType} (required), a {@code String} with the FQN of the type of the document to use.</li>
 * </ul>
 * <p>
 * It is also possible to use the {@link MongoItemWriter} programmatically via
 * {@link #MongoItemWriter(com.mongodb.client.MongoClient, String, String, Class)}.
 *
 * @param <T> the type to write
 */
@Named("mongoItemWriter")
public class MongoItemWriter<T> extends MongoItemBase<T> implements ItemWriter<T> {

    @Inject
    MongoItemWriter(
            @BatchProperty(name = "mongoClient") String mongoClient,
            @BatchProperty(name = "database") String database,
            @BatchProperty(name = "collection") String collection,
            @BatchProperty(name = "itemType") Class<T> itemType) {
        super(mongoClient, database, collection, itemType);
    }

    /**
     * Constructs a new {@link MongoItemWriter}.
     *
     * @param mongoClient a {@link MongoClient} to read the data
     * @param database a {@code String} with the MongoDB database to retrieve the data
     * @param collection a {@code String} with the MongoDB collection to retrieve the data
     * @param itemType a {@code Class} with the type of the document to map into the result object
     */
    public MongoItemWriter(final MongoClient mongoClient, final String database, final String collection,
            final Class<T> itemType) {
        super(mongoClient, database, collection, itemType);
    }

    @Override
    public void write(List<T> items) {
        TransactionManager transactionManager = Arc.container().select(TransactionManager.class).get();
        TransactionSynchronizationRegistry registry = Arc.container().select(TransactionSynchronizationRegistry.class).get();
        if (registry.getTransactionStatus() == Status.STATUS_ACTIVE) {
            ClientSession clientSession = getMongoClient().startSession();
            clientSession.startTransaction();
            registry.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {

                }

                @Override
                public void afterCompletion(int status) {
                    try {
                        if (transactionManager.getStatus() == Status.STATUS_ROLLEDBACK) {
                            try (clientSession) {
                                clientSession.abortTransaction();
                            }
                        } else {
                            try (clientSession) {
                                clientSession.commitTransaction();
                            }
                        }
                    } catch (SystemException e) {
                        throw new TransactionalException("There was a problem committing the Mongo transaction", e);
                    }
                }
            });

            getMongoCollection().insertMany(clientSession, items);
        }
    }
}
