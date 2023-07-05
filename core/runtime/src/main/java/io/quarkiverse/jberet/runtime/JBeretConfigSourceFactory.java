package io.quarkiverse.jberet.runtime;

import static java.util.Collections.singletonList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jberet.job.model.Decision;
import org.jberet.job.model.Flow;
import org.jberet.job.model.InheritableJobElement;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.Properties;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Split;
import org.jberet.job.model.Step;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;

public class JBeretConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        final Set<String> properties = new HashSet<>();
        final List<Job> jobs = JBeretDataHolder.getData().getJobs();
        for (Job job : jobs) {
            addConfigNames(properties, job);

            for (JobElement jobElement : job.getJobElements()) {
                addConfigNames(properties, jobElement);
            }

            for (final InheritableJobElement inheritingJobElement : job.getInheritingJobElements()) {
                addConfigNames(properties, inheritingJobElement);
            }
        }

        return singletonList(new JBeretConfigSource(properties));
    }

    private static void addConfigNames(final Set<String> properties, final JobElement jobElement) {
        if (jobElement instanceof Step) {
            final Step step = (Step) jobElement;
            addConfigNames(properties, step);

            addConfigNames(properties, step.getBatchlet());

            if (step.getChunk() != null) {
                addConfigNames(properties, step.getChunk().getReader());
                addConfigNames(properties, step.getChunk().getProcessor());
                addConfigNames(properties, step.getChunk().getWriter());
                addConfigNames(properties, step.getChunk().getCheckpointAlgorithm());
            }

            if (step.getPartition() != null) {
                addConfigNames(properties, step.getPartition().getMapper());
                addConfigNames(properties, step.getPartition().getCollector());
                addConfigNames(properties, step.getPartition().getAnalyzer());
                addConfigNames(properties, step.getPartition().getReducer());
            }
        }

        if (jobElement instanceof Flow) {
            final Flow flow = (Flow) jobElement;
            addConfigNames(properties, flow);

            for (JobElement flowElement : flow.getJobElements()) {
                addConfigNames(properties, flowElement);
            }
        }

        if (jobElement instanceof Split) {
            final Split split = (Split) jobElement;
            addConfigNames(properties, split.getProperties());
            for (final Flow flow : split.getFlows()) {
                addConfigNames(properties, (JobElement) flow);
            }
        }

        if (jobElement instanceof Decision) {
            final Decision decision = (Decision) jobElement;
            addConfigNames(properties, decision.getProperties());
        }
    }

    private static void addConfigNames(final Set<String> properties, final InheritableJobElement inheritableJobElement) {
        if (inheritableJobElement != null) {
            if (inheritableJobElement.getProperties() != null) {
                addConfigNames(properties, inheritableJobElement.getProperties());
            }

            if (inheritableJobElement.getListeners() != null) {
                for (RefArtifact refArtifact : inheritableJobElement.getListeners().getListeners()) {
                    addConfigNames(properties, refArtifact);
                }
            }
        }
    }

    private static void addConfigNames(final Set<String> properties, final RefArtifact refArtifact) {
        if (refArtifact != null && refArtifact.getProperties() != null) {
            addConfigNames(properties, refArtifact.getProperties());
        }
    }

    private static void addConfigNames(final Set<String> properties, final Properties propertiesHolder) {
        if (propertiesHolder != null) {
            properties.addAll(propertiesHolder.getNameValues().keySet());
        }
    }
}
