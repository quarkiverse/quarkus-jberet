package io.quarkiverse.jberet.runtime;

import jakarta.batch.operations.JobOperator;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

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
    @Singleton
    public QuarkusJobOperator quarkusJobOperator() {
        return (QuarkusJobOperator) jobOperator();
    }

    @Produces
    @DefaultBean
    @Singleton
    public JobRepository jobRepository() {
        return ((AbstractJobOperator) jobOperator()).getJobRepository();
    }

    @Produces
    @DefaultBean
    @Singleton
    public JobScheduler jobScheduler() {
        return JobScheduler.getJobScheduler();
    }
}
