package io.quarkiverse.jberet.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.jberet.job.model.Job;

import io.quarkus.arc.Unremovable;

@Unremovable
@ApplicationScoped
public class JobsProducer {
    @Inject
    BeanManager beanManager;

    public List<Job> getJobs() {
        List<Job> jobs = new ArrayList<>();
        Set<Bean<?>> beans = beanManager.getBeans(Job.class, Any.Literal.INSTANCE);
        for (Bean<?> bean : beans) {
            String name = bean.getName();
            Job job = (Job) beanManager.getReference(bean, Job.class, beanManager.createCreationalContext(bean));
            job.setJobXmlName(name);
            jobs.add(job);
        }
        return jobs;
    }
}
