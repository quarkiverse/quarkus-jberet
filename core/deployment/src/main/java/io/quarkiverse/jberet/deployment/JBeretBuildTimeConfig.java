package io.quarkiverse.jberet.deployment;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkiverse.jberet.runtime.JBeretConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.util.GlobUtil;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;

@ConfigMapping(prefix = JBeretConfig.PREFIX)
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface JBeretBuildTimeConfig {
    /**
     * The JBeret Jobs configuration.
     */
    JobsConfig jobs();

    interface JobsConfig {
        /**
         * A regex pattern of Job names to exclude.
         */
        Optional<List<@WithConverter(RegexPatternConverter.class) Pattern>> includes();

        /**
         * A regex pattern of Job names to include.
         */
        Optional<List<@WithConverter(RegexPatternConverter.class) Pattern>> excludes();

        class RegexPatternConverter implements Converter<Pattern> {
            @Override
            public Pattern convert(final String value) throws IllegalArgumentException, NullPointerException {
                return Pattern.compile(GlobUtil.toRegexPattern(value));
            }
        }
    }
}
