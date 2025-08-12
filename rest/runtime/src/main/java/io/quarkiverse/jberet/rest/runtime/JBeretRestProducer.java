package io.quarkiverse.jberet.rest.runtime;

import static io.quarkus.vertx.http.runtime.VertxHttpConfig.InsecureRequests.ENABLED;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jberet.rest.client.BatchClient;

import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils;

public class JBeretRestProducer {
    @Inject
    VertxHttpBuildTimeConfig httpBuildTimeConfig;
    @Inject
    VertxHttpConfig httpConfig;

    @Produces
    @DefaultBean
    @Singleton
    public BatchClient batchClient() {
        VertxHttpConfig.InsecureRequests insecureRequestStrategy = HttpServerOptionsUtils
                .getInsecureRequestStrategy(httpConfig, httpBuildTimeConfig, LaunchMode.current());
        String scheme = insecureRequestStrategy.equals(ENABLED) ? "http" : "https";
        return new BatchClient(scheme + "://" + httpConfig.host() + ":" + httpConfig.port());
    }
}
