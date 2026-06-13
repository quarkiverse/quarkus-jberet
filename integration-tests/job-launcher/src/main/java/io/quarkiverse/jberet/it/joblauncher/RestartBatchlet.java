package io.quarkiverse.jberet.it.joblauncher;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Named
@Dependent
public class RestartBatchlet extends AbstractBatchlet {
    static final AtomicBoolean FIRST_RUN = new AtomicBoolean(true);

    @Override
    public String process() {
        if (FIRST_RUN.compareAndSet(true, false)) {
            throw new RuntimeException("Simulated failure on first run");
        }
        return BatchStatus.COMPLETED.toString();
    }
}
