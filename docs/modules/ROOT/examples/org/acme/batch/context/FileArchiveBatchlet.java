package org.acme.batch.context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Named
public class FileArchiveBatchlet extends AbstractBatchlet {
    @Inject
    JobContext jobContext;
    @Inject
    StepContext stepContext;
    @ConfigProperty(name = "directory", defaultValue = "/tmp")
    String directory;

    @Override
    public String process() throws Exception {
        Path archive = Files.createDirectories(Paths.get(directory, "archive"));
        Integer archived = (Integer) stepContext.getPersistentUserData(); // <1>
        int count = archived != null ? archived : 0;

        try (Stream<Path> files = Files.list(Paths.get(directory))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Files.move(file, archive.resolve(file.getFileName()));
                stepContext.setPersistentUserData(++count); // <2>
            }
        }

        jobContext.setTransientUserData(count); // <3>
        return BatchStatus.COMPLETED.toString();
    }
}
