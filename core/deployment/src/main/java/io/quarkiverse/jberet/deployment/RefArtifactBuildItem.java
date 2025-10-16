package io.quarkiverse.jberet.deployment;

import org.jberet.job.model.RefArtifact;
import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class RefArtifactBuildItem extends MultiBuildItem {
    private final RefArtifact refArtifact;
    private final ClassInfo resolvedArtifact;

    public RefArtifactBuildItem(RefArtifact refArtifact, ClassInfo resolvedArtifact) {
        this.refArtifact = refArtifact;
        this.resolvedArtifact = resolvedArtifact;
    }

    public RefArtifact getRefArtifact() {
        return refArtifact;
    }

    public ClassInfo getResolvedArtifact() {
        return resolvedArtifact;
    }
}
