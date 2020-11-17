package io.quarkiverse.jberet.rest.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jberet.rest.client.BatchClient;

import io.quarkus.arc.DefaultBean;

public class JBeretRestProducer {
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
