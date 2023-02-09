package io.quarkiverse.jberet.runtime;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkiverse.jberet.runtime.JBeretConfig.Repository.Type;

public class RepositoryTypeConverter extends AbstractEnumFallbackConverter<Type> implements Converter<Type> {

    public RepositoryTypeConverter() {
        super(Type.class);
    }

    @Override
    protected Type getFallback() {
        return Type.OTHER;
    }

}
