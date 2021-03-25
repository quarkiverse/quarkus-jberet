package io.quarkiverse.jberet.it.jdbc;

import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.BatchStatus;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
@Named
public class JdbcBatchlet extends AbstractBatchlet {
    @ConfigProperty(name = "jdbc.batchlet.fail", defaultValue = "false")
    Boolean fail;

    @Override
    public String process() {
        if (fail) {
            throw new RuntimeException();
        }

        return BatchStatus.COMPLETED.toString();
    }
}
