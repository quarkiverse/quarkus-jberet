package io.quarkiverse.jberet.it.client;

import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.BatchStatus;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

@Named
@Dependent
public class ClientBatchlet extends AbstractBatchlet {
    @Override
    public String process() {
        return BatchStatus.COMPLETED.toString();
    }
}
