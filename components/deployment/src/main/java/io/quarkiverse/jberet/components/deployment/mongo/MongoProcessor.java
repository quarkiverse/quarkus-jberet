package io.quarkiverse.jberet.components.deployment.mongo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jberet.job.model.Properties;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

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
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.mongodb.deployment.MongoClientNameBuildItem;

class MongoProcessor {
    private static final DotName MONGO_CLIENT_ANNOTATION = DotName.createSimple(MongoClientName.class.getName());

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
            BuildProducer<MongoClientNameBuildItem> mongoClients) {

        if (capabilities.isMissing(Capability.MONGODB_CLIENT)) {
            return;
        }

        List<String> mongoClientNames = new ArrayList<>();
        Collection<AnnotationInstance> mongoClientAnnotations = combinedIndex.getIndex()
                .getAnnotations(MONGO_CLIENT_ANNOTATION);
        for (AnnotationInstance annotation : mongoClientAnnotations) {
            if (annotation.value() != null && !annotation.value().toString().isEmpty()) {
                mongoClientNames.add(annotation.value().asString());
            }
        }

        Collection<ClassInfo> mongoItems = combinedIndex.getIndex().getAllKnownSubclasses(MongoItemBase.class);
        // Force to create a MongoClient without an injection point, since it can be referenced in the reader / writer
        for (RefArtifactBuildItem refArtifact : refArtifacts) {
            if (mongoItems.contains(refArtifact.getResolvedArtifact())) {
                Properties properties = refArtifact.getRefArtifact().getProperties();
                if (properties != null) {
                    String mongoClientName = properties.get("mongoClient");
                    if (mongoClientName != null && !mongoClientNames.contains(mongoClientName)) {
                        mongoClients.produce(new MongoClientNameBuildItem(mongoClientName));
                    }
                }
            }
        }
    }
}
