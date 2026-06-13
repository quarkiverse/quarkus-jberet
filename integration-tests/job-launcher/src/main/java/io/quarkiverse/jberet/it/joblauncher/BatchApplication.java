package io.quarkiverse.jberet.it.joblauncher;

import java.util.Properties;

import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.inject.Inject;

import io.quarkiverse.jberet.runtime.QuarkusJobLauncher;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class BatchApplication implements QuarkusApplication {
    @Inject
    QuarkusJobLauncher jobLauncher;

    @Override
    public int run(String... args) throws Exception {
        if (args.length > 0 && "restart".equals(args[0])) {
            return restart();
        }
        return start();
    }

    private int start() throws Exception {
        JobExecution execution = jobLauncher.run("main", new Properties());
        return execution.getBatchStatus() == BatchStatus.COMPLETED ? 0 : 1;
    }

    private int restart() throws Exception {
        JobExecution firstExecution = jobLauncher.run("restart", new Properties());
        if (firstExecution.getBatchStatus() != BatchStatus.FAILED) {
            return 1;
        }

        JobExecution restartExecution = jobLauncher.run("restart", new Properties());
        return restartExecution.getBatchStatus() == BatchStatus.COMPLETED ? 0 : 1;
    }
}
