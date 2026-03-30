package io.quarkiverse.jberet.components.runtime.item.mongo;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.BatchProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;

import io.quarkiverse.jberet.runtime.api.ItemReader;

/**
 * Reads data from a MongoDB collection using a MongoDB cursor.
 * <p>
 * The {@link MongoCursorItemReader} can be referenced in the Job XMl definition by the name
 * <code>mongoItemReader</code>. It supports the following properties:
 * <ul>
 * <li>{@code name} (optional), a {@code String} reference to the CDI name of the {@link MongoClient}.
 * If not specified, the default (unnamed) MongoClient is used.</li>
 * <li>{@code database} (required), a {@code String} with the MongoDB database to use.</li>
 * <li>{@code collection} (required), a {@code String} with the MongoDB collection to use.</li>
 * <li>{@code itemType} (required), a {@code String} with the FQN of the type of the document to use.</li>
 * <li>{@code filter}, see {@link MongoCursorItemReader#setFilter(Bson)}</li>
 * <li>{@code projection}, see {@link MongoCursorItemReader#setProjection(Bson)}</li>
 * <li>{@code sort}, see {@link MongoCursorItemReader#setSort(Bson)}</li>
 * <li>{@code hint}, see {@link MongoCursorItemReader#setHint(Bson)}</li>
 * <li>{@code limit}, see {@link MongoCursorItemReader#setLimit(Integer)}</li>
 * <li>{@code skip}, see {@link MongoCursorItemReader#setSkip(Integer)}</li>
 * <li>{@code maxTime}, see {@link MongoCursorItemReader#setMaxTime(Duration)}</li>
 * <li>{@code batchSize}, see {@link MongoCursorItemReader#setBatchSize(Integer)}</li>
 * </ul>
 * <p>
 * It is also possible to use the {@link MongoCursorItemReader} programmatically via
 * {@link #MongoCursorItemReader(com.mongodb.client.MongoClient, String, String, Class)}.
 *
 * @param <T> the read result type
 */
@Named("mongoItemReader")
public class MongoCursorItemReader<T> extends MongoItemBase<T> implements ItemReader<T> {
    @BatchProperty
    Bson filter;
    @BatchProperty
    Bson projection;
    @BatchProperty
    Bson sort;
    @BatchProperty
    Bson hint;
    @BatchProperty
    Integer limit;
    @BatchProperty
    Integer skip;
    @BatchProperty
    Duration maxTime;
    @BatchProperty
    Integer batchSize;

    @Inject
    MongoCursorItemReader(
            @BatchProperty(name = "mongoClient") String mongoClient,
            @BatchProperty(name = "database") String database,
            @BatchProperty(name = "collection") String collection,
            @BatchProperty(name = "itemType") Class<T> itemType) {
        super(mongoClient, database, collection, itemType);
    }

    /**
     * Constructs a new {@link MongoCursorItemReader}.
     *
     * @param mongoClient a {@link MongoClient} to read the data
     * @param database a {@code String} with the MongoDB database to retrieve the data
     * @param collection a {@code String} with the MongoDB collection to retrieve the data
     * @param itemType a {@code Class} with the type of the document to map into the result object
     */
    public MongoCursorItemReader(final MongoClient mongoClient, final String database, final String collection,
            final Class<T> itemType) {
        super(mongoClient, database, collection, itemType);
    }

    private MongoCursor<T> cursor;
    private int currentItem;

    @Override
    public void open(Serializable checkpoint) {
        super.open(checkpoint);
        FindIterable<T> iterable = getMongoCollection().find();

        if (filter != null) {
            iterable.filter(filter);
        }
        if (projection != null) {
            iterable.projection(projection);
        }
        if (sort != null) {
            iterable.sort(sort);
        }
        if (hint != null) {
            iterable.hint(hint);
        }
        if (limit != null && limit > 0) {
            iterable.limit(limit);
        }
        if (checkpoint != null) {
            iterable.skip((int) checkpoint);
        } else if (skip != null && skip > 0) {
            iterable.skip(skip);
            currentItem = skip;
        }
        if (maxTime != null && maxTime.toMillis() > 0) {
            iterable.maxTime(maxTime.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (batchSize != null && batchSize > 0) {
            iterable.batchSize(batchSize);
        }

        this.cursor = iterable.iterator();
    }

    @Override
    public T read() {
        if (cursor.hasNext()) {
            T next = cursor.next();
            currentItem++;
            return next;
        }
        return null;
    }

    @Override
    public void close() {
        cursor.close();
    }

    @Override
    public Serializable checkpointInfo() {
        return currentItem;
    }

    /**
     * Sets the query filter to apply to the query.
     *
     * @param filter the filter, which may be null.
     * @return this {@link MongoCursorItemReader}
     * @see com.mongodb.client.FindIterable#filter(org.bson.conversions.Bson)
     */
    public MongoCursorItemReader<T> setFilter(Bson filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Sets a document describing the fields to return for all matching documents.
     *
     * @param projection the project document, which may be null.
     * @return this {@link MongoCursorItemReader}
     * @see com.mongodb.client.model.Projections
     * @see com.mongodb.client.FindIterable#projection(org.bson.conversions.Bson)
     */
    public MongoCursorItemReader<T> setProjection(Bson projection) {
        this.projection = projection;
        return this;
    }

    /**
     * Sets the sort criteria to apply to the query.
     *
     * @param sort the sort criteria, which may be null.
     * @return this {@link MongoCursorItemReader}
     * @see com.mongodb.client.FindIterable#sort(org.bson.conversions.Bson)
     */
    public MongoCursorItemReader<T> setSort(Bson sort) {
        this.sort = sort;
        return this;
    }

    /**
     * Sets the hint for which index to use. A null value means no hint is set.
     *
     * @param hint the hint
     * @return this {@link MongoCursorItemReader}
     * @see com.mongodb.client.FindIterable#hint(org.bson.conversions.Bson)
     */
    public MongoCursorItemReader<T> setHint(Bson hint) {
        this.hint = hint;
        return this;
    }

    /**
     * Sets the limit to apply.
     *
     * @param limit the limit, which may be 0
     * @return this {@link MongoCursorItemReader}
     * @see com.mongodb.client.FindIterable#limit(int)
     */
    public MongoCursorItemReader<T> setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets the number of documents to skip.
     *
     * @param skip the number of documents to skip
     * @return this {@link MongoCursorItemReader}
     * @see com.mongodb.client.FindIterable#skip(int)
     */
    public MongoCursorItemReader<T> setSkip(Integer skip) {
        this.skip = skip;
        return this;
    }

    /**
     * Sets the maximum execution time on the server for this operation.
     *
     * @param maxTime the max time as a {@link java.time.Duration}
     * @return this {@link MongoCursorItemReader}
     * @see com.mongodb.client.FindIterable#maxTime(long, java.util.concurrent.TimeUnit)
     */
    public MongoCursorItemReader<T> setMaxTime(Duration maxTime) {
        this.maxTime = maxTime;
        return this;
    }

    /**
     * Sets the number of documents to return per batch.
     *
     * @param batchSize the batch size
     * @return this {@link MongoCursorItemReader}
     * @see com.mongodb.client.FindIterable#batchSize(int)
     */
    public MongoCursorItemReader<T> setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
        return this;
    }
}
