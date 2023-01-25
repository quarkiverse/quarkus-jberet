package io.quarkiverse.jberet.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.jberet")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface JBeretConfig {
    /**
     *
     */
    Map<String, JobConfig> job();

    /**
     *
     */
    JobsConfig jobs();

    /**
     *
     */
    Repository repository();

    interface JobConfig {
        /**
         *
         */
        Optional<String> cron();

        /**
         *
         */
        Map<String, String> params();
    }

    interface JobsConfig {
        /**
         *
         */
        Optional<List<String>> includes();

        /**
         *
         */
        Optional<List<String>> excludes();
    }

    interface Repository {
        /**
         *
         */
        @WithDefault("IN_MEMORY")
        String type();

        /**
         *
         */
        Jdbc jdbc();

        interface Jdbc {
            /**
             *
             */
            @WithDefault(DataSourceUtil.DEFAULT_DATASOURCE_NAME)
            String datasource();

            /**
             * Allow custom DDL file resource for JBeret tables creation;
             * if using <b>custom table names</b> please also set <code>sql-filename</code>
             * property to propagate table names
             */
            @WithName("ddl-file")
            Optional<String> ddlFileName();

            /**
             * Allow custom queries to be used to query JBeret tables;
             * this is mandatory if custom table names are used in
             * custom DDL filename
             */
            @WithName("sql-file")
            Optional<String> sqlFileName();

            /** If present, prefix JBeret tables with this property value */
            @WithName("db-table-prefix")
            Optional<String> dbTablePrefix();

            /** If present, suffix JBeret tables with this property value */
            @WithName("db-table-suffix")
            Optional<String> dbTableSuffix();
        }
    }
}
