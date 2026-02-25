package io.quarkiverse.jberet.components.runtime.item.mongo;

import java.lang.annotation.Annotation;

import jakarta.enterprise.inject.Default;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.mongodb.runtime.MongoConfig;

public final class MongoClients {
    private MongoClients() {
        throw new UnsupportedOperationException();
    }

    public static MongoClient getMongoClient(final String mongoClientName) {
        Annotation qualifier;
        if (mongoClientName == null || MongoConfig.isDefaultClient(mongoClientName)) {
            qualifier = Default.Literal.INSTANCE;
        } else {
            qualifier = MongoClientName.Literal.of(mongoClientName);
        }
        InjectableInstance<MongoClient> instance = Arc.container().select(MongoClient.class, qualifier);
        return instance.getActive();
    }
}
