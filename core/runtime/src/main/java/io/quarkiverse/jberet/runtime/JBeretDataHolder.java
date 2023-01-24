package io.quarkiverse.jberet.runtime;

import java.util.List;
import java.util.Map;

import org.jberet.job.model.Job;

public class JBeretDataHolder {
    private static volatile JBeretData data;

    static void registerJobs(final List<Job> jobs, final Map<String, String> batchArtifactsAliases) {
        data = new JBeretData(jobs, batchArtifactsAliases);
    }

    static JBeretData getData() {
        return data;
    }

    static class JBeretData {
        private final List<Job> jobs;
        private final Map<String, String> batchArtifactsAliases;

        public JBeretData(final List<Job> jobs, final Map<String, String> batchArtifactsAliases) {
            this.jobs = jobs;
            this.batchArtifactsAliases = batchArtifactsAliases;
        }

        public List<Job> getJobs() {
            return jobs;
        }

        public Map<String, String> getBatchArtifactsAliases() {
            return batchArtifactsAliases;
        }
    }
}
