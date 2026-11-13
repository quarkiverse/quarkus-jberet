package io.quarkiverse.jberet.deployment;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.listener.AbstractStepListener;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class InheritedStepListenersTest {
    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(InheritedStepBatchlet.class, CountingStepListener.class)
                    .addAsManifestResource("inherited-step.xml", "batch-jobs/inherited-step.xml")
                    .addAsResource(new StringAsset("quarkus.jberet.job.step-listeners=countingStepListener"),
                            "application.properties"));

    @Test
    public void listenersAddedOnce() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("inherited-step", new Properties());
        await().atMost(5, SECONDS)
                .until(() -> BatchStatus.COMPLETED.equals(jobOperator.getJobExecution(executionId).getBatchStatus()));

        assertEquals(1, InheritedStepBatchlet.COUNT.get());
        assertEquals(1, CountingStepListener.COUNT.get());
    }

    @Named
    @Dependent
    public static class InheritedStepBatchlet extends AbstractBatchlet {
        static final AtomicInteger COUNT = new AtomicInteger();

        @Override
        public String process() {
            COUNT.incrementAndGet();
            return BatchStatus.COMPLETED.toString();
        }
    }

    @Named
    @Dependent
    public static class CountingStepListener extends AbstractStepListener {
        static final AtomicInteger COUNT = new AtomicInteger();

        @Override
        public void beforeStep() {
            COUNT.incrementAndGet();
        }
    }
}
