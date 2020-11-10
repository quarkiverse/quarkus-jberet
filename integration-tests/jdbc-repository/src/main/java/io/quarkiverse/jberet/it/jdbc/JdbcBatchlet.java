package io.quarkiverse.jberet.it.jdbc;

import javax.batch.api.Batchlet;
import javax.batch.runtime.BatchStatus;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

@Dependent
@Named
public class JdbcBatchlet implements Batchlet {
    @Override
    public String process() throws Exception {
        return BatchStatus.COMPLETED.toString();
    }

    @Override
    public void stop() throws Exception {

    }
}
