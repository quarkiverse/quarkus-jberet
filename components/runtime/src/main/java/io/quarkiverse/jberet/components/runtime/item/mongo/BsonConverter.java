package io.quarkiverse.jberet.components.runtime.item.mongo;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.spi.Converter;

public class BsonConverter implements Converter<Bson> {
    @Override
    public Bson convert(String value) throws IllegalArgumentException, NullPointerException {
        return Document.parse(value);
    }
}
