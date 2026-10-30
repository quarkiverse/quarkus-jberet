package io.quarkiverse.jberet.components.runtime.repository;

import static io.quarkus.mongodb.runtime.MongoClientBeanUtil.mongoClient;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jberet.repository.JobRepository;

import io.quarkiverse.jberet.components.runtime.repository.JobRepositoryConfig.Repository.Mongodb;
import io.quarkiverse.jberet.runtime.JobRepositorySupplier;

@Singleton
public class MongoJobRepositorySupplier implements JobRepositorySupplier {
    @Inject
    protected JobRepositoryConfig config;

    @Override
    public JobRepository get() {
        Mongodb mongodb = config.repository().mongodb();
        return new MongoJobRepository(mongoClient(mongodb.clientName()), mongodb.database());
    }

    @Override
    public String getName() {
        return Mongodb.TYPE;
    }
}
