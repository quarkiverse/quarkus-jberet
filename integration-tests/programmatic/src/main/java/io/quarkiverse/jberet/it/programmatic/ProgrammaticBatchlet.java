package io.quarkiverse.jberet.it.programmatic;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Named
@Dependent
public class ProgrammaticBatchlet extends AbstractBatchlet {
    @Override
    public String process() throws Exception {
        return BatchStatus.COMPLETED.toString();
    }
}
