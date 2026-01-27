package io.quarkiverse.jberet.components.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class ComponentsProcessor {
    @BuildStep
    public void registerExtension(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem("jberet-components"));
    }
}
