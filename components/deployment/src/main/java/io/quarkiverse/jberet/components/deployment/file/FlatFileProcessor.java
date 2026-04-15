package io.quarkiverse.jberet.components.deployment.file;

import io.quarkiverse.jberet.components.runtime.item.file.FlatFileItemReader;
import io.quarkiverse.jberet.components.runtime.item.file.FlatFileItemWriter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

class FlatFileProcessor {
    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(FlatFileItemReader.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(FlatFileItemWriter.class));
    }
}
