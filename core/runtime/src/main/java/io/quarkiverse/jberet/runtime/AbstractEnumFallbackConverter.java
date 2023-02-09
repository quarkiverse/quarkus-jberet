package io.quarkiverse.jberet.runtime;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.runtime.configuration.HyphenateEnumConverter;

public abstract class AbstractEnumFallbackConverter<T extends Enum<T>> implements Converter<T> {

    protected final Converter<T> delegate;

    public AbstractEnumFallbackConverter(Class<T> enumType) {
        delegate = HyphenateEnumConverter.of(enumType);
    }

    protected abstract T getFallback();

    @Override
    public T convert(String string) throws IllegalArgumentException, NullPointerException {
        try {
            return delegate.convert(string);
        } catch (IllegalArgumentException | NullPointerException e) {
            return getFallback();
        }
    }

}
