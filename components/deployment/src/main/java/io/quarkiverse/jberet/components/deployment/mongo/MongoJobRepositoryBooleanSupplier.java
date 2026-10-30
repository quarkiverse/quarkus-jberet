package io.quarkiverse.jberet.components.deployment.mongo;

import java.util.function.BooleanSupplier;

import io.quarkiverse.jberet.components.runtime.repository.JobRepositoryConfig.Repository.Mongodb;
import io.quarkiverse.jberet.runtime.JBeretConfig;

public class MongoJobRepositoryBooleanSupplier implements BooleanSupplier {
    JBeretConfig config;

    public MongoJobRepositoryBooleanSupplier(JBeretConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return Mongodb.TYPE.equals(config.repository().type());
    }
}
