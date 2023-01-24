package io.quarkiverse.jberet.deployment;

import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.runtime.BatchStatus;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

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
