package io.quarkiverse.jberet.components.deployment.jdbc;

import io.quarkiverse.jberet.components.runtime.item.jdbc.JdbcBatchItemWriter;
import io.quarkiverse.jberet.components.runtime.item.jdbc.JdbcCursorItemReader;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

class JdbcProcessor {
    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(JdbcCursorItemReader.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(JdbcBatchItemWriter.class));
    }
}
