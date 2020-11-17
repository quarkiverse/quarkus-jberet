package io.quarkiverse.jberet.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "jberet", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class JBeretConfig {
    /**
     *
     */
    @ConfigItem
    public Map<String, JobConfig> job;

    /**
     *
     */
    @ConfigItem
    public JobsConfig jobs;

    /**
     *
     */
    @ConfigItem
    public Repository repository;

    @ConfigGroup
    public static class JobConfig {
        /**
         *
         */
        @ConfigItem
        public Optional<String> cron;

        /**
         *
         */
        @ConfigItem
        public Map<String, String> params;
    }

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

    @ConfigGroup
    public static class Repository {
        /**
         *
         */
        @ConfigItem(defaultValue = "in-memory")
        public Type type;

        /**
         *
         */
        @ConfigItem
        public Jdbc jdbc;

        @ConfigGroup
        public static class Jdbc {
            /**
             *
             */
            @ConfigItem(defaultValue = DataSourceUtil.DEFAULT_DATASOURCE_NAME)
            public String datasource;
        }

        public enum Type {
            IN_MEMORY,
            JDBC
        }
    }
}
