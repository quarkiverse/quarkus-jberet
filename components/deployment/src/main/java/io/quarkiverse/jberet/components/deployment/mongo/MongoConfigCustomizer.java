package io.quarkiverse.jberet.components.deployment.mongo;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class MongoConfigCustomizer implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(SmallRyeConfigBuilder builder) {
        builder.withDefaultValue("quarkus.mongodb.force-default-clients", "true");
    }
}
