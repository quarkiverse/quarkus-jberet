package io.quarkiverse.jberet.rest.deployment;

import static io.quarkus.deployment.Capability.RESTEASY_JSON_JACKSON;
import static io.quarkus.deployment.Capability.RESTEASY_JSON_JACKSON_CLIENT;
import static io.quarkus.deployment.Capability.RESTEASY_JSON_JSONB;
import static io.quarkus.deployment.Capability.RESTEASY_JSON_JSONB_CLIENT;
import static io.quarkus.deployment.Capability.RESTEASY_REACTIVE_JSON_JACKSON;
import static io.quarkus.deployment.Capability.RESTEASY_REACTIVE_JSON_JSONB;
import static io.quarkus.deployment.Capability.REST_CLIENT_REACTIVE_JACKSON;
import static io.quarkus.deployment.Capability.REST_CLIENT_REACTIVE_JSONB;

import io.quarkiverse.jberet.rest.runtime.JBeretRestProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

class JBeretRestProcessor {
    @BuildStep
    void registerExtension(Capabilities capabilities, BuildProducer<FeatureBuildItem> feature) {
        boolean isRestClassicPresent = capabilities.isPresent(RESTEASY_JSON_JACKSON)
                && capabilities.isPresent(RESTEASY_JSON_JACKSON_CLIENT) ||
                capabilities.isPresent(RESTEASY_JSON_JSONB) && capabilities.isPresent(RESTEASY_JSON_JSONB_CLIENT);

        boolean isRestReactivePresent = capabilities.isPresent(RESTEASY_REACTIVE_JSON_JACKSON)
                && capabilities.isPresent(REST_CLIENT_REACTIVE_JACKSON) ||
                capabilities.isPresent(RESTEASY_REACTIVE_JSON_JSONB) && capabilities.isPresent(REST_CLIENT_REACTIVE_JSONB);

        if (!isRestClassicPresent && !isRestReactivePresent) {
            throw new IllegalStateException("""
                    JBeret REST Client requires a Quarkus REST Extension, but none could be found. \
                    Please either add `quarkus-rest-jackson` and `quarkus-rest-client-jackson` (recommended), \
                    or add `quarkus-resteasy-jackson` and `quarkus-resteasy-client-jackson` to the project dependencies.""");
        }

        feature.produce(new FeatureBuildItem("jberet-rest"));
    }

    @BuildStep
    void indexJBeretRest(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.jberet", "jberet-rest-api"));
    }

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(JBeretRestProducer.class));
    }
}
