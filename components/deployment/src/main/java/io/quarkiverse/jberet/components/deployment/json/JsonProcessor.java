package io.quarkiverse.jberet.components.deployment.json;

import java.util.List;

import org.jberet.job.model.Properties;
import org.jboss.jandex.DotName;

import io.quarkiverse.jberet.components.runtime.item.json.JsonItemReader;
import io.quarkiverse.jberet.components.runtime.item.json.JsonItemWriter;
import io.quarkiverse.jberet.deployment.RefArtifactBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class JsonProcessor {
    private static final DotName JSON_ITEM_READER = DotName.createSimple(JsonItemReader.class);

    @BuildStep
    void additionalBeans(
            Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        if (capabilities.isMissing(Capability.JACKSON)) {
            return;
        }

        additionalBeans.produce(new AdditionalBeanBuildItem(JsonItemReader.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(JsonItemWriter.class));
    }

    @BuildStep
    void nativeImage(
            Capabilities capabilities,
            List<RefArtifactBuildItem> refArtifacts,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        if (capabilities.isMissing(Capability.JACKSON)) {
            return;
        }

        // Register the itemType referenced in the Job XML, to be loaded and deserialized by Jackson
        for (RefArtifactBuildItem refArtifact : refArtifacts) {
            if (refArtifact.getResolvedArtifact() == null
                    || !JSON_ITEM_READER.equals(refArtifact.getResolvedArtifact().name())) {
                continue;
            }
            Properties properties = refArtifact.getRefArtifact().getProperties();
            String itemType = properties != null ? properties.get("itemType") : null;
            // skip expressions, which can only be resolved at runtime
            if (itemType != null && !itemType.isBlank() && !itemType.contains("#{")) {
                reflectiveClasses.produce(ReflectiveClassBuildItem.builder(itemType.trim())
                        .constructors().methods().fields().build());
            }
        }
    }
}
