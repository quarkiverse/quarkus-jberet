package io.quarkiverse.jberet.components.deployment.mongo;

import java.util.Collection;
import java.util.List;

import org.jberet.job.model.Properties;
import org.jboss.jandex.ClassInfo;

import io.quarkiverse.jberet.components.runtime.item.mongo.MongoConfigBuilder;
import io.quarkiverse.jberet.components.runtime.item.mongo.MongoCursorItemReader;
import io.quarkiverse.jberet.components.runtime.item.mongo.MongoItemBase;
import io.quarkiverse.jberet.components.runtime.item.mongo.MongoItemWriter;
import io.quarkiverse.jberet.deployment.RefArtifactBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigBuilderBuildItem;
import io.quarkus.mongodb.deployment.spi.MongoClientBuildItem;

class MongoProcessor {
    @BuildStep
    void config(
            Capabilities capabilities,
            BuildProducer<StaticInitConfigBuilderBuildItem> staticInitConfigBuilder,
            BuildProducer<RunTimeConfigBuilderBuildItem> runtimeConfigBuilder) {

        if (capabilities.isMissing(Capability.MONGODB_CLIENT)) {
            return;
        }

        staticInitConfigBuilder.produce(new StaticInitConfigBuilderBuildItem(MongoConfigBuilder.class));
        runtimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(MongoConfigBuilder.class));
    }

    @BuildStep
    void additionalBeans(
            Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        if (capabilities.isMissing(Capability.MONGODB_CLIENT)) {
            return;
        }

        additionalBeans.produce(new AdditionalBeanBuildItem(MongoCursorItemReader.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(MongoItemWriter.class));
    }

    @BuildStep
    void clients(
            Capabilities capabilities,
            List<RefArtifactBuildItem> refArtifacts,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<MongoClientBuildItem> mongoClients) {

        if (capabilities.isMissing(Capability.MONGODB_CLIENT)) {
            return;
        }

        Collection<ClassInfo> mongoItems = combinedIndex.getIndex().getAllKnownSubclasses(MongoItemBase.class);
        // Force to create a MongoClient without an injection point, since it can be referenced in the reader / writer
        // The client is always marked as unremovable, even if it also has injection points, because the reader / writer
        // looks up the blocking MongoClient, which may not be registered if only the ReactiveMongoClient is injected
        for (RefArtifactBuildItem refArtifact : refArtifacts) {
            if (mongoItems.contains(refArtifact.getResolvedArtifact())) {
                Properties properties = refArtifact.getRefArtifact().getProperties();
                String mongoClientName = properties != null ? properties.get("mongoClient") : null;
                if (mongoClientName != null) {
                    mongoClients.produce(MongoClientBuildItem.ofUnremovable(mongoClientName));
                } else {
                    mongoClients.produce(MongoClientBuildItem.defaultClientUnremovable());
                }
            }
        }
    }
}
