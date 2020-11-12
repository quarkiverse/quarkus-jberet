package io.quarkiverse.jberet.runtime;

import javax.batch.operations.JobOperator;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jberet.operations.AbstractJobOperator;
import org.jberet.repository.JobRepository;
import org.jberet.rest.client.BatchClient;
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
    private String host;
    @ConfigProperty(name = "quarkus.http.port")
    private int port;
    @ConfigProperty(name = "quarkus.http.insecure-requests")
    private String insecure;

    @Produces
    @DefaultBean
    @Singleton
    public BatchClient batchClient() {
        final String scheme = "enabled".equals(insecure) ? "http" : "https";
        return new BatchClient(scheme + "://" + host + ":" + port);
    }
}
