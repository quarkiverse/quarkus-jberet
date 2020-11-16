package io.quarkiverse.jberet.runtime;

import java.util.List;

import org.jberet.job.model.Job;
import org.jberet.schedule.JobScheduleConfig;

public class JBeretDataHolder {
    private static volatile JBeretData data;

    static void registerJobs(final List<Job> jobs, final List<JobSchedule> schedules) {
        data = new JBeretData(jobs, schedules);
    }

    static List<Job> getJobs() {
        return data.jobs;
    }

    static List<JobSchedule> getSchedules() {
        return data.schedules;
    }

    private static class JBeretData {
        private final List<Job> jobs;
        private final List<JobSchedule> schedules;

        public JBeretData(final List<Job> jobs, final List<JobSchedule> schedules) {
            this.jobs = jobs;
            this.schedules = schedules;
        }
    }

    public static class JobSchedule {
        private JobScheduleConfig config;
        private String cron;

        public JobSchedule() {
        }

        public JobSchedule(final JobScheduleConfig config, final String cron) {
            this.config = config;
            this.cron = cron;
        }

        public JobScheduleConfig getConfig() {
            return config;
        }

        public void setConfig(final JobScheduleConfig config) {
            this.config = config;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(final String cron) {
            this.cron = cron;
        }
    }
}
