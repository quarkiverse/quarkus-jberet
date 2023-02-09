package io.quarkiverse.jberet.jpa.job.repository;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkiverse.jberet.jpa.job.repository.JBeretJpaJobRepositoryConfig.Repository.Type;
import io.quarkiverse.jberet.runtime.AbstractEnumFallbackConverter;

public class RepositoryTypeConverter extends AbstractEnumFallbackConverter<Type> implements Converter<Type> {

    public RepositoryTypeConverter() {
        super(Type.class);
    }

    @Override
    protected Type getFallback() {
        return Type.OTHER;
    }

}
