package io.quarkiverse.jberet.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "jberet", phase = ConfigPhase.BUILD_TIME)
public class JBeretBuildTimeConfig {
    /**
     *
     */
    @ConfigItem
    public JobsConfig jobs;

    @ConfigGroup
    public static class JobsConfig {
        /**
         *
         */
        @ConfigItem
        public Optional<List<String>> includes;
        /**
         *
         */
        @ConfigItem
        public Optional<List<String>> excludes;
    }
}
