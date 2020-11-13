package io.quarkiverse.jberet.it.scheduler;

import javax.batch.api.Batchlet;
import javax.batch.runtime.BatchStatus;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

@Dependent
@Named
public class SchedulerBatchlet implements Batchlet {
    @Override
    public String process() {
        return BatchStatus.COMPLETED.toString();
    }

    @Override
    public void stop() {

    }
}
