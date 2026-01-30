package io.quarkiverse.jberet.runtime;

import static io.quarkiverse.jberet.runtime.JBeretConfig.JobConfig.DEFAULT;
import static io.quarkus.datasource.common.runtime.DataSourceUtil.DEFAULT_DATASOURCE_NAME;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.jberet.job.model.Job;
import org.jberet.job.model.Listeners;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Step;

import io.quarkiverse.jberet.runtime.JobProcessor.JobProcessorBuilder;
import io.quarkiverse.jberet.runtime.repository.InMemoryJobRepositorySupplier;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = JBeretConfig.PREFIX)
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface JBeretConfig {
    String PREFIX = "quarkus.jberet";

    /**
     * The JBeret Jobs configuration by name.
     */
    @WithUnnamedKey(DEFAULT)
    @WithDefaults
    @ConfigDocMapKey("job-name")
    Map<String, JobConfig> job();

    /**
     * The JBeret Repository configuration.
     */
    @ConfigDocSection(generated = true)
    Repository repository();

    interface JobConfig {
        String DEFAULT = "<default>";

        /**
         * A list of {@link jakarta.batch.api.listener.JobListener} bean names or FQNs to execute with the Job.
         * <p>
         * The unnamed configuration <code>quarkus.jberet.job.job-listeners</code>, applies to all configured jobs.
         */
        Optional<List<RefArtifact>> jobListeners();

        /**
         * A list of {@link jakarta.batch.api.listener.StepListener} bean names or FQNs to execute with the Job.
         * <p>
         * The unnamed configuration <code>quarkus.jberet.job.step-listeners</code>, applies to all configured jobs.
         */
        Optional<List<RefArtifact>> stepListeners();

        default JobProcessor listeners() {
            return new JobProcessorBuilder()
                    .jobConsumer(new Consumer<Job>() {
                        @Override
                        public void accept(Job job) {
                            List<RefArtifact> listeners = jobListeners().orElse(emptyList());
                            if (job.getListeners() == null) {
                                job.setListeners(new Listeners());
                            }
                            job.getListeners().getListeners().addAll(listeners);
                        }
                    })
                    .stepConsumer(new Consumer<Step>() {
                        @Override
                        public void accept(Step step) {
                            List<RefArtifact> listeners = stepListeners().orElse(emptyList());
                            if (step.getListeners() == null) {
                                step.setListeners(new Listeners());
                            }
                            step.getListeners().getListeners().addAll(listeners);
                        }
                    })
                    .build();
        }
    }

    interface Repository {
        /**
         * The repository type to store JBeret and Job data. A <code>jdbc</code> jdbc type requires a JDBC datasource.
         */
        @WithConverter(SkewerConverter.class)
        @WithDefault(InMemoryJobRepositorySupplier.TYPE)
        String type();

        /**
         * The JBeret JDBC Repository configuration.
         */
        Jdbc jdbc();

        interface Jdbc {
            /**
             * The datasource name for the JBeret Repository. By default, it uses the default (unnamed) datasource.
             */
            @WithDefault(DEFAULT_DATASOURCE_NAME)
            String datasource();

            /**
             * Custom DDL file resource for JBeret tables creation; if using <b>custom table names</b> please
             * also set <code>sql-file</code> property to propagate table names.
             */
            @WithName("ddl-file")
            @WithConverter(TrimmedStringConverter.class)
            Optional<String> ddlFileName();

            /**
             * Custom queries to be used to query JBeret tables; this is mandatory if custom table names are used
             * in custom DDL filename.
             * <p>
             * The file must be of type <code>properties</code>, and must follow the exact template as defined in
             * <a href=
             * "https://raw.githubusercontent.com/jberet/jsr352/refs/tags/3.1.0.Final/jberet-core/src/main/resources/sql/jberet-sql.properties">jberet.properties</a>
             */
            @WithName("sql-file")
            @WithConverter(TrimmedStringConverter.class)
            Optional<String> sqlFileName();
        }
    }
}
