package io.quarkiverse.jberet.it.cdiscopes;

import jakarta.inject.Inject;

import org.jberet.testapps.common.AbstractIT;
import org.junit.jupiter.api.BeforeEach;

import io.quarkiverse.jberet.runtime.QuarkusJobOperator;

/**
 * Quarkus version of JBeret {@link AbstractIT}
 */
public abstract class AbstractQuarkusIT extends AbstractIT {
    @Inject
    QuarkusJobOperator quarkusJobOperator;

    @BeforeEach
    @Override
    public void before() throws Exception {
        super.jobOperator = new JobOperatorImplQuarkusDelegate(quarkusJobOperator);
    }
}
