package io.quarkiverse.jberet.components.deployment.jpa;

import static io.quarkiverse.jberet.components.runtime.repository.JobRepositoryConfig.Repository.*;

import java.util.function.BooleanSupplier;

import io.quarkiverse.jberet.runtime.JBeretConfig;

public class JpaJobRepositoryBooleanSupplier implements BooleanSupplier {
    JBeretConfig config;

    public JpaJobRepositoryBooleanSupplier(JBeretConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return Jpa.TYPE.equals(config.repository().type());
    }
}
