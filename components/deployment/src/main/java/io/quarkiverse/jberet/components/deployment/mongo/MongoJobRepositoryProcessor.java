package io.quarkiverse.jberet.components.deployment.mongo;

import io.quarkiverse.jberet.components.runtime.repository.JobRepositoryConfig;
import io.quarkiverse.jberet.components.runtime.repository.MongoJobRepositorySupplier;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.mongodb.deployment.spi.MongoClientBuildItem;

class MongoJobRepositoryProcessor {
    @BuildStep(onlyIf = MongoJobRepositoryBooleanSupplier.class)
    void additionalBeans(
            Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        if (capabilities.isMissing(Capability.MONGODB_CLIENT)) {
            throw new IllegalStateException("""
                    JBeret Components MongoDB Repository requires Quarkus MongoDB Client Extension. Please add
                    `quarkus-mongodb-client` to the project dependencies.""");
        }

        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(MongoJobRepositorySupplier.class));
    }

    @BuildStep(onlyIf = MongoJobRepositoryBooleanSupplier.class)
    void client(
            Capabilities capabilities,
            JobRepositoryConfig jobRepositoryConfig,
            BuildProducer<MongoClientBuildItem> mongoClients) {

        if (capabilities.isMissing(Capability.MONGODB_CLIENT)) {
            return;
        }

        // Force to create a MongoClient without an injection point, since the repository looks it up
        mongoClients.produce(MongoClientBuildItem.ofUnremovable(jobRepositoryConfig.repository().mongodb().clientName()));
    }
}
