package io.quarkiverse.jberet.runtime;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.common.utils.StringUtil;

public class SkewerConverter implements Converter<String> {
    @Override
    public String convert(final String value) throws IllegalArgumentException, NullPointerException {
        return StringUtil.skewer(value.trim());
    }
}
