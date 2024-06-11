package io.quarkiverse.jberet.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class BatchArtifactBuildItem extends MultiBuildItem {
    private final String className;
    private final String named;
    private final String ref;

    public BatchArtifactBuildItem(final String className, final String named, final String ref) {
        this.className = className;
        this.named = named;
        this.ref = ref;
    }

    public String getClassName() {
        return className;
    }

    public String getNamed() {
        return named;
    }

    public String getRef() {
        return ref;
    }
}
