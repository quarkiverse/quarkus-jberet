package io.quarkiverse.jberet.components.runtime.item.mongo;

import org.bson.conversions.Bson;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class MongoConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withConverter(Bson.class, 100, new BsonConverter());
    }
}
