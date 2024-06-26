package io.quarkiverse.jberet.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jberet.tools.AbstractJobXmlResolver;

import io.quarkus.runtime.util.ClassPathUtils;

public class QuarkusJobXmlResolver extends AbstractJobXmlResolver {
    private final Set<String> jobXmlNames = new HashSet<>();

    public QuarkusJobXmlResolver(final JBeretBuildTimeConfig config, final ClassLoader classLoader) throws IOException {
        ClassPathUtils.consumeAsPaths(classLoader, DEFAULT_PATH, path -> {
            jobXmlNames.addAll(findBatchFilesFromPath(path, config));
        });
    }

    @Override
    public Collection<String> getJobXmlNames(final ClassLoader classLoader) {
        return jobXmlNames;
    }

    @Override
    public InputStream resolveJobXml(final String jobXml, final ClassLoader classLoader) throws IOException {
        return super.resolveJobXml(DEFAULT_PATH + jobXml, classLoader);
    }

    private static Set<String> findBatchFilesFromPath(final Path path, final JBeretBuildTimeConfig config) {
        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile)
                    .map(file -> file.getFileName().toString())
                    .filter(file -> file.endsWith(".xml"))
                    .filter(new IncludesFilter(config))
                    .filter(new ExcludesFilter(config))
                    .map(file -> file.substring(0, file.length() - 4)).collect(Collectors.toSet());
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    private static class IncludesFilter implements Predicate<String> {
        private final List<Pattern> includes;

        public IncludesFilter(final JBeretBuildTimeConfig config) {
            this.includes = config.jobs().includes().orElse(new ArrayList<>());
        }

        @Override
        public boolean test(final String file) {
            if (includes.isEmpty()) {
                return true;
            } else {
                return includes.stream().anyMatch(p -> p.matcher(file).matches());
            }
        }
    }

    private static class ExcludesFilter implements Predicate<String> {
        private final List<Pattern> excludes;

        public ExcludesFilter(final JBeretBuildTimeConfig config) {
            this.excludes = config.jobs().excludes().orElse(new ArrayList<>());
        }

        @Override
        public boolean test(final String file) {
            if (excludes.isEmpty()) {
                return true;
            } else {
                return excludes.stream().noneMatch(p -> p.matcher(file).matches());
            }
        }
    }
}
