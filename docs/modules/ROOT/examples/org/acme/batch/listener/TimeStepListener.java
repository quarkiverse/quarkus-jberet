package org.acme.batch.listener;

import jakarta.batch.api.listener.AbstractStepListener;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.logging.Logger;

@Named
public class TimeStepListener extends AbstractStepListener {
    private static final Logger log = Logger.getLogger("org.acme.batch");

    @Inject
    StepContext stepContext;

    private long startTime;

    @Override
    public void beforeStep() {
        startTime = System.currentTimeMillis();
    }

    @Override
    public void afterStep() {
        long duration = System.currentTimeMillis() - startTime;
        String stepName = stepContext.getStepName();
        log.info("Step " + stepName + " took " + duration + " ms");
    }
}
