package io.quarkiverse.jberet.rest.runtime;

import static io.quarkus.vertx.http.runtime.HttpConfiguration.InsecureRequests.ENABLED;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jberet.rest.client.BatchClient;

import io.quarkus.arc.DefaultBean;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.HttpConfiguration.InsecureRequests;
import io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils;

public class JBeretRestProducer {
    @Inject
    HttpBuildTimeConfig httpBuildTimeConfig;
    @Inject
    HttpConfiguration httpConfiguration;

    @Produces
    @DefaultBean
    @Singleton
    public BatchClient batchClient() {
        InsecureRequests insecureRequestStrategy = HttpServerOptionsUtils.getInsecureRequestStrategy(httpBuildTimeConfig,
                httpConfiguration.insecureRequests);
        String scheme = insecureRequestStrategy.equals(ENABLED) ? "http" : "https";
        return new BatchClient(scheme + "://" + httpConfiguration.host + ":" + httpConfiguration.port);
    }
}
