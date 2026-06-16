package io.quarkiverse.jberet.components.deployment.jpa;

import io.quarkiverse.jberet.components.runtime.item.jpa.JpaItemReader;
import io.quarkiverse.jberet.components.runtime.item.jpa.JpaItemWriter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

class JpaProcessor {
    @BuildStep
    void additionalBeans(
            Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        if (capabilities.isMissing(Capability.HIBERNATE_ORM)) {
            return;
        }

        additionalBeans.produce(new AdditionalBeanBuildItem(JpaItemReader.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(JpaItemWriter.class));
    }
}
