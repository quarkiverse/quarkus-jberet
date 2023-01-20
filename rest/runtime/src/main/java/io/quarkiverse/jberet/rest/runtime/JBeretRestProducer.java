package io.quarkiverse.jberet.rest.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jberet.rest.client.BatchClient;

import io.quarkus.arc.DefaultBean;

public class JBeretRestProducer {
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
}
