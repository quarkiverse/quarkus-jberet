package org.acme.batch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Named
public class FileCleanupBatchlet extends AbstractBatchlet {
    @ConfigProperty
    String directory;
    @ConfigProperty
    int daysToKeep;

    @Override
    public String process() throws Exception {
        Path dirPath = Paths.get(directory);
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);

        try (Stream<Path> files = Files.walk(dirPath)) {
            for (Path path : files.filter(Files::isRegularFile).toList()) {
                if (Files.getLastModifiedTime(path).toMillis() < cutoffTime) {
                    Files.delete(path);
                }
            }
        }

        return BatchStatus.COMPLETED.toString();
    }
}
