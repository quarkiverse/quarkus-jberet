package io.quarkiverse.jberet.deployment;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import com.google.inject.Inject;

@ApplicationScoped
@Named("artifactBatchlet")
public class ArtifactBatchlet implements Batchlet {
    @Inject
    @BatchProperty(name = "name")
    String name;

    @Override
    public String process() {
        if (!name.equals("david")) {
            throw new RuntimeException("Unexpected value injected to 'name': " + name);
        }
        return BatchStatus.COMPLETED.toString();
    }

    @Override
    public void stop() {
    }
}
