package io.quarkiverse.jberet.components.runtime.item.mongo;

import java.io.Serializable;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

public abstract class MongoItemBase<T> {
    private final MongoClient mongoClient;
    private final String database;
    private final String collection;
    private final Class<T> itemType;

    private MongoCollection<T> mongoCollection;

    protected MongoItemBase(
            String mongoClient,
            String database,
            String collection,
            Class<T> itemType) {
        this.mongoClient = MongoClients.getMongoClient(mongoClient);
        this.database = database;
        this.collection = collection;
        this.itemType = itemType;
    }

    protected MongoItemBase(final MongoClient mongoClient, final String database, final String collection,
            final Class<T> itemType) {
        this.mongoClient = mongoClient;
        this.database = database;
        this.collection = collection;
        this.itemType = itemType;
    }

    public void open(Serializable checkpoint) {
        mongoCollection = mongoClient.getDatabase(database).getCollection(collection, itemType);
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongoCollection<T> getMongoCollection() {
        if (mongoCollection == null) {
            throw new IllegalStateException("MongoCollection not open");
        }
        return mongoCollection;
    }
}
