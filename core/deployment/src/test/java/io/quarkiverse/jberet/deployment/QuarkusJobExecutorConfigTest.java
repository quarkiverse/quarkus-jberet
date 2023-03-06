package io.quarkiverse.jberet.deployment;

import static org.awaitility.Awaitility.await;

import java.time.LocalTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.batch.api.Batchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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

public class QuarkusJobExecutorConfigTest {

    @RegisterExtension
    // TODO: Think about better communication as 2 indicates one worker thread for job execution
    // - could add 1 for orchestration thread -> Needs to be documented well
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ConfigParamsTest.ConfigBatchlet.class)
                    .addAsResource(new StringAsset("quarkus.jberet.max-async=1"),
                            "application.properties"));

    @Inject
    QuarkusJobOperator quarkusJobOperator;

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

    @Test
    void runsJobsConsecutivelyWhenThreadLimitIsOne() {
        List<Long> jobIds = Stream.of("job 1", "job 2", "job 3")
                .map(jobName -> new JobBuilder(jobName)
                        .step(new StepBuilder("dummyStep")
                                .batchlet("blocking")
                                .build())
                        .build())
                .map(job -> quarkusJobOperator.start(job, new Properties()))
                .collect(Collectors.toList());

        await("First job finished").atMost(1, TimeUnit.SECONDS).until(() -> jobIds.stream()
                .map(jobId -> quarkusJobOperator.getJobExecution(jobId).getBatchStatus())
                .filter(BatchStatus.COMPLETED::equals)
                .count() == 1);
        await("Second job finished").atMost(1, TimeUnit.SECONDS).until(() -> jobIds.stream()
                .map(jobId -> quarkusJobOperator.getJobExecution(jobId).getBatchStatus())
                .filter(BatchStatus.COMPLETED::equals)
                .count() == 2);
        await("Third job finished").atMost(1, TimeUnit.SECONDS).until(() -> jobIds.stream()
                .map(jobId -> quarkusJobOperator.getJobExecution(jobId).getBatchStatus())
                .filter(BatchStatus.COMPLETED::equals)
                .count() == 3);
    }
}
