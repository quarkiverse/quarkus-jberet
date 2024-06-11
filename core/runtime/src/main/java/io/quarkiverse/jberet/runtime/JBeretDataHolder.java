package io.quarkiverse.jberet.runtime;

import java.util.List;

import org.jberet.job.model.Job;

public class JBeretDataHolder {
    private static volatile JBeretData data;

    static void registerJobs(final List<Job> jobs) {
        data = new JBeretData(jobs);
    }

    static JBeretData getData() {
        return data;
    }

    static class JBeretData {
        private final List<Job> jobs;

        public JBeretData(final List<Job> jobs) {
            this.jobs = jobs;
        }

        public List<Job> getJobs() {
            return jobs;
        }
    }
}
