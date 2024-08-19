package io.quarkiverse.jberet.runtime;

import static io.quarkus.datasource.common.runtime.DataSourceUtil.*;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = JBeretConfig.PREFIX)
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface JBeretConfig {
    String PREFIX = "quarkus.jberet";

    /**
     * The JBeret Jobs configuration by name.
     */
    Map<String, JobConfig> job();

    /**
     * The JBeret Repository configuration.
     */
    Repository repository();

    /**
     * The maximum number of threads allowed to be executed.
     */
    Optional<Integer> maxAsync();

    interface JobConfig {
        /**
         * The Job schedule in Cron format, see <a href="https://en.wikipedia.org/wiki/Cron">cron</a>.
         */
        Optional<String> cron();

        /**
         * The Job parameters.
         */
        Map<String, String> params();
    }

    interface Repository {
        /**
         * The repository type to store JBeret and Job data. A <code>jdbc</code>jdbc type requires a JDBC datasource.
         */
        @WithConverter(SkewerConverter.class)
        @WithDefault(JBeretInMemoryJobRepositoryProducer.TYPE)
        String type();

        /**
         * The JBeret JDBC Repository configuration.
         */
        Jdbc jdbc();

        interface Jdbc {
            /**
             * The datasource name for the JBeret Repository.
             */
            @WithDefault(DEFAULT_DATASOURCE_NAME)
            String datasource();

            /**
             * Custom DDL file resource for JBeret tables creation; if using <b>custom table names</b> please
             * also set <code>sql-filename</code> property to propagate table names.
             */
            @WithName("ddl-file")
            Optional<String> ddlFileName();

            /**
             * Custom queries to be used to query JBeret tables; this is mandatory if custom table names are used
             * in custom DDL filename.
             */
            @WithName("sql-file")
            Optional<String> sqlFileName();

            /**
             * JBeret tables name prefix.
             */
            @WithName("db-table-prefix")
            Optional<String> dbTablePrefix();

            /**
             * IJBeret tables name suffix.
             */
            @WithName("db-table-suffix")
            Optional<String> dbTableSuffix();
        }
    }
}
