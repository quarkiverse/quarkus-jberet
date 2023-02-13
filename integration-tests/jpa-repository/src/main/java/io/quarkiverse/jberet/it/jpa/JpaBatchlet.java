package io.quarkiverse.jberet.it.jpa;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
@Named
public class JpaBatchlet extends AbstractBatchlet {
    @ConfigProperty(name = "jpa.batchlet.fail", defaultValue = "false")
    Boolean fail;

    @Override
    public String process() {
        if (fail) {
            throw new RuntimeException();
        }

        return BatchStatus.COMPLETED.toString();
    }
}
