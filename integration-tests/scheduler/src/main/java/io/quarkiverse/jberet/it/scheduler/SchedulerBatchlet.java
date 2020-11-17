package io.quarkiverse.jberet.it.scheduler;

import javax.batch.api.Batchlet;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
@Named
public class SchedulerBatchlet implements Batchlet {
    @Inject
    @ConfigProperty(name = "status", defaultValue = "")
    String status;

    @Override
    public String process() {
        return status;
    }

    @Override
    public void stop() {

    }
}
