package io.quarkiverse.jberet.deployment;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.batch.api.Batchlet;
import javax.batch.api.listener.AbstractJobListener;
import javax.batch.runtime.BatchStatus;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.StepBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.jberet.runtime.QuarkusJobOperator;
import io.quarkus.logging.Log;
import io.quarkus.test.QuarkusUnitTest;

public class MaxAsyncJobExecutorConfigTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ConfigParamsTest.ConfigBatchlet.class)
                    .addAsResource(new StringAsset("quarkus.jberet.max-async=1"),
                            "application.properties"));

    @Inject
    QuarkusJobOperator quarkusJobOperator;

    @Test
    void runsJobsConsecutivelyWhenThreadLimitIsOne() {
        List<Long> jobIds = Stream.of("job 1", "job 2", "job 3")
                .map(jobName -> new JobBuilder(jobName)
                        .listener("threadJobListener")
                        .step(new StepBuilder("dummyStep")
                                .batchlet("blocking")
                                .build())
                        .build())
                .map(job -> quarkusJobOperator.start(job, new Properties()))
                .collect(Collectors.toList());

        await("All jobs finished").atMost(3, TimeUnit.SECONDS).until(() -> jobIds.stream()
                .map(jobId -> quarkusJobOperator.getJobExecution(jobId).getBatchStatus())
                .filter(BatchStatus.COMPLETED::equals)
                .count() == 3);

        assertTrue(ThreadCounter.maxParallelRunningJobCounter > 0);
        assertTrue(ThreadCounter.maxParallelRunningJobCounter < 2);
    }

    @Named("blocking")
    @Dependent
    public static class BlockingBatchlet implements Batchlet {

        @Override
        public String process() throws InterruptedException {
            Log.debug(LocalTime.now() + " Executing");
            Thread.sleep(700);
            Log.debug(LocalTime.now() + " Execution finished");
            return BatchStatus.COMPLETED.toString();
        }

        @Override
        public void stop() {
        }
    }

    @Named
    @Dependent
    public static class ThreadJobListener extends AbstractJobListener {
        @Override
        public void beforeJob() {
            ThreadCounter.incrementJobCounter();
        }

        @Override
        public void afterJob() {
            ThreadCounter.decrementJobCounter();
        }
    }

    public static class ThreadCounter {
        private static volatile int runningJobsCounter = 0;
        private static volatile int maxParallelRunningJobCounter = 0;

        public static synchronized void incrementJobCounter() {
            runningJobsCounter++;
            if (runningJobsCounter > maxParallelRunningJobCounter) {
                maxParallelRunningJobCounter = runningJobsCounter;
            }

        }

        public static synchronized void decrementJobCounter() {
            runningJobsCounter--;
        }
    }
}
