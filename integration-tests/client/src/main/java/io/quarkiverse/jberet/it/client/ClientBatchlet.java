package io.quarkiverse.jberet.it.client;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Named
@Dependent
public class ClientBatchlet extends AbstractBatchlet {
    @Override
    public String process() {
        return BatchStatus.COMPLETED.toString();
    }
}
