package io.quarkiverse.jberet.it.programmatic;

import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.BatchStatus;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

@Named
@Dependent
public class ProgrammaticBatchlet extends AbstractBatchlet {
    @Override
    public String process() throws Exception {
        return BatchStatus.COMPLETED.toString();
    }
}
