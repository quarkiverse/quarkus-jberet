package io.quarkiverse.jberet.components.runtime.repository;

import static io.quarkus.mongodb.runtime.MongoClientBeanUtil.mongoClient;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jberet.repository.JobRepository;

import io.quarkiverse.jberet.components.runtime.repository.JobRepositoryConfig.Repository.Mongodb;
import io.quarkiverse.jberet.runtime.JobRepositorySupplier;
import io.quarkus.mongodb.runtime.MongoConfig;

@Singleton
public class MongoJobRepositorySupplier implements JobRepositorySupplier {
    private static final String DEFAULT_DATABASE = "jberet";

    @Inject
    protected JobRepositoryConfig config;
    @Inject
    protected MongoConfig mongoConfig;

    @Override
    public JobRepository get() {
        String clientName = config.repository().mongodb().clientName();
        String database = mongoConfig.clients().get(clientName).database().orElse(DEFAULT_DATABASE);
        return new MongoJobRepository(mongoClient(clientName), database);
    }

    @Override
    public String getName() {
        return Mongodb.TYPE;
    }
}
