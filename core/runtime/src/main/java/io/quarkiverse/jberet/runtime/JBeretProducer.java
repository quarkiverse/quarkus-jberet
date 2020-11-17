package io.quarkiverse.jberet.runtime;

import javax.batch.operations.JobOperator;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jberet.operations.AbstractJobOperator;
import org.jberet.repository.JobRepository;
import org.jberet.rest.client.BatchClient;
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

    @ConfigProperty(name = "quarkus.http.host")
    String host;
    @ConfigProperty(name = "quarkus.http.port")
    int port;
    @ConfigProperty(name = "quarkus.http.insecure-requests")
    String insecure;

    @Produces
    @DefaultBean
    @Singleton
    public BatchClient batchClient() {
        final String scheme = "enabled".equals(insecure) ? "http" : "https";
        return new BatchClient(scheme + "://" + host + ":" + port);
    }

    @Produces
    @DefaultBean
    @Singleton
    public JobScheduler jobScheduler() {
        return JobScheduler.getJobScheduler();
    }
}
