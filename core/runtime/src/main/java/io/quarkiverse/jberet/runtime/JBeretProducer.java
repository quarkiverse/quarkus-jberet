package io.quarkiverse.jberet.runtime;

import javax.batch.operations.JobOperator;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jberet.operations.AbstractJobOperator;
import org.jberet.repository.JobRepository;
import org.jberet.schedule.JobScheduler;
import org.jberet.spi.JobOperatorContext;

import io.quarkus.arc.DefaultBean;

public class JBeretProducer {
    @Produces
    @DefaultBean
    @Singleton
    public JobOperator jobOperator() {
        return JobOperatorContext.getJobOperatorContext().getJobOperator();
    }

    @Produces
    @DefaultBean
    @Singleton
    public JobRepository jobRepository() {
        return ((AbstractJobOperator) JobOperatorContext.getJobOperatorContext().getJobOperator()).getJobRepository();
    }

    @Produces
    @DefaultBean
    @Singleton
    public JobScheduler jobScheduler() {
        return JobScheduler.getJobScheduler();
    }
}
