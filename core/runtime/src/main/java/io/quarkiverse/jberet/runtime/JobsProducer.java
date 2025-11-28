package io.quarkiverse.jberet.runtime;

import static io.quarkiverse.jberet.runtime.JBeretConfig.JobConfig.DEFAULT;

import java.util.List;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import org.jberet.job.model.Job;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.Unremovable;

@Unremovable
@ApplicationScoped
public class JobsProducer {
    @Inject
    JBeretConfig config;
    @Inject
    @Any
    InjectableInstance<Job> jobs;

    public List<Job> getJobs() {
        JobProcessor globalListeners = config.job().get(DEFAULT).listeners();
        return jobs.handlesStream()
                .map(new Function<InstanceHandle<Job>, Job>() {
                    @Override
                    public Job apply(InstanceHandle<Job> jobInstanceHandle) {
                        Job job = jobInstanceHandle.get();
                        job.setJobXmlName(jobInstanceHandle.getBean().getName());
                        globalListeners.processJob(job);
                        config.job().get(jobInstanceHandle.getBean().getName()).listeners().processJob(job);
                        return job;
                    }
                }).toList();
    }
}
