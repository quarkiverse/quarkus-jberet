package io.quarkiverse.jberet.it.scheduler;

import jakarta.batch.api.Batchlet;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
@Named
public class SchedulerBatchlet implements Batchlet {
    @Inject
    @ConfigProperty(name = "status", defaultValue = "FAILED")
    String status;

    @Override
    public String process() {
        return status;
    }

    @Override
    public void stop() {

    }
}
