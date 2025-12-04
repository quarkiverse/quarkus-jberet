package io.quarkiverse.jberet.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;

@ConfigMapping(prefix = JBeretConfig.PREFIX)
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface JBeretRuntimeConfig {
    /**
     * The maximum number of threads allowed to be executed.
     */
    Optional<Integer> maxAsync();

    /**
     * The JBeret Jobs configuration by name.
     */
    @WithDefaults
    @ConfigDocMapKey("job-name")
    Map<String, JobConfig> job();

    interface JobConfig {
        /**
         * The Job schedule in Cron format.
         * <p>
         * The syntax used for Cron expressions is based on Quartz. See
         * <a href="https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html">Cron Trigger</a>.
         */
        Optional<String> cron();

        /**
         * The Job parameters.
         */
        Map<String, String> params();

        default Properties paramsAsProperties() {
            Properties properties = new Properties();
            properties.putAll(params());
            return properties;
        }
    }
}
