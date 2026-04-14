package io.quarkiverse.jberet.components.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

class ComponentsProcessor {
    @BuildStep
    public void registerExtension(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem("jberet-components"));
    }

    /**
     * Index this artifact to query for implementations of Batch Components. We exclude it from Bean Discovery via
     * configuration, since we want to selectively register beans based on what is in use.
     */
    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        indexDependencies.produce(new IndexDependencyBuildItem("io.quarkiverse.jberet", "quarkus-jberet-components"));
    }
}
