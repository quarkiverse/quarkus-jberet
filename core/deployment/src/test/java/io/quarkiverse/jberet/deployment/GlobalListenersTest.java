package io.quarkiverse.jberet.deployment;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.listener.JobListener;
import jakarta.batch.api.listener.StepListener;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.StepBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.jberet.deployment.RefArtifactTest.RefArtifactBatchlet;
import io.quarkus.test.QuarkusUnitTest;

public class GlobalListenersTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RefArtifactBatchlet.class)
                    .addAsManifestResource("global-listeners.xml", "batch-jobs/global-listeners.xml")
                    .addAsResource(new StringAsset("""
                            quarkus.jberet.job.job-listeners=globalJobListener
                            quarkus.jberet.job.step-listeners=globalStepListener
                            quarkus.jberet.job.global-listeners.job-listeners=namedJobListener
                            quarkus.jberet.job.global-listeners.step-listeners=namedStepListener
                            """), "application.properties"));

    @Test
    public void globalListeners() throws Exception {
        JobOperator jobOperator = BatchRuntime.getJobOperator();

        long executionId = jobOperator.start("global-listeners", new Properties());
        await().atMost(5, SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });

        long anotherId = jobOperator.start("producer-listeners", new Properties());
        await().atMost(5, SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(anotherId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });

        assertTrue(GlobalJobListener.latch.await(0, SECONDS));
        assertTrue(GlobalStepListener.latch.await(0, SECONDS));
        assertTrue(NamedJobListener.latch.await(0, SECONDS));
        assertTrue(NamedStepListener.latch.await(0, SECONDS));
        assertTrue(ProducerJobListener.latch.await(0, SECONDS));
        assertTrue(ProducerStepListener.latch.await(0, SECONDS));
    }

    @Named("batchlet")
    @Dependent
    public static class DummyBatchlet extends AbstractBatchlet {
        @Override
        public String process() {
            return BatchStatus.COMPLETED.toString();
        }
    }

    @Named
    @ApplicationScoped
    public static class GlobalJobListener implements JobListener {
        static CountDownLatch latch = new CountDownLatch(4);

        @Override
        public void beforeJob() {
            latch.countDown();
        }

        @Override
        public void afterJob() {
            latch.countDown();
        }
    }

    @Named
    @ApplicationScoped
    public static class GlobalStepListener implements StepListener {
        static CountDownLatch latch = new CountDownLatch(8);

        @Override
        public void beforeStep() {
            latch.countDown();
        }

        @Override
        public void afterStep() {
            latch.countDown();
        }
    }

    @Named
    @ApplicationScoped
    public static class NamedJobListener implements JobListener {
        static CountDownLatch latch = new CountDownLatch(2);

        @Override
        public void beforeJob() {
            latch.countDown();
        }

        @Override
        public void afterJob() {
            latch.countDown();
        }
    }

    @Named
    @ApplicationScoped
    public static class NamedStepListener implements StepListener {
        static CountDownLatch latch = new CountDownLatch(4);

        @Override
        public void beforeStep() {
            latch.countDown();
        }

        @Override
        public void afterStep() {
            latch.countDown();
        }
    }

    @ApplicationScoped
    public static class JobProducer {
        @Produces
        @Named("producer-listeners")
        public Job job() {
            return new JobBuilder("producer-listeners")
                    .listener("producerJobListener")
                    .step(new StepBuilder("step").listener("producerStepListener").batchlet("batchlet").next("another").build())
                    .step(new StepBuilder("another").listener("producerStepListener").batchlet("batchlet").build())
                    .build();
        }
    }

    @ApplicationScoped
    @Named
    public static class ProducerJobListener implements JobListener {
        static CountDownLatch latch = new CountDownLatch(2);

        @Override
        public void beforeJob() {
            latch.countDown();
        }

        @Override
        public void afterJob() {
            latch.countDown();
        }
    }

    @ApplicationScoped
    @Named
    public static class ProducerStepListener implements StepListener {
        static CountDownLatch latch = new CountDownLatch(4);

        @Override
        public void beforeStep() {
            latch.countDown();
        }

        @Override
        public void afterStep() {
            latch.countDown();
        }
    }
}
