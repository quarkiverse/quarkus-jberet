package io.quarkiverse.jberet.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class BatchArtifactBuildItem extends MultiBuildItem {
    private final String className;
    private final String named;
    private final String alias;

    public BatchArtifactBuildItem(final String className, final String named, final String alias) {
        this.className = className;
        this.named = named;
        this.alias = alias;
    }

    public String getClassName() {
        return className;
    }

    public String getNamed() {
        return named;
    }

    public String getAlias() {
        return alias;
    }
}
