package io.quarkiverse.jberet.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.batch.operations.JobOperator;
import jakarta.inject.Inject;

import org.jberet.repository.JobRepository;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.jberet.runtime.QuarkusJobOperator;
import io.quarkus.test.QuarkusUnitTest;

public class JBeretInjectionTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    JobOperator jobOperator;
    @Inject
    JobRepository jobRepository;
    @Inject
    QuarkusJobOperator quarkusJobOperator;

    @Test
    void injection() {
        assertNotNull(jobOperator);
        assertNotNull(jobRepository);

        assertTrue(jobOperator instanceof QuarkusJobOperator);
        assertNotNull(quarkusJobOperator);
    }
}
