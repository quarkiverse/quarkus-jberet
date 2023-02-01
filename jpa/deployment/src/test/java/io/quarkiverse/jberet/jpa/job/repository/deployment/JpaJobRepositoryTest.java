package io.quarkiverse.jberet.jpa.job.repository.deployment;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jberet.repository.JobRepository;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.jberet.jpa.job.repository.JBeretJpaJobRepository;
import io.quarkus.test.QuarkusUnitTest;

public class JpaJobRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(BatchEntityManager.class)
                            .addAsResource(
                                    new StringAsset(
                                            String.join(
                                                    System.lineSeparator(),
                                                    Arrays.asList(
                                                            "quarkus.jberet.repository.type=other",
                                                            "quarkus.jberet.repository.jpa.persistence-unit-name=batch"))),
                                    "application.properties"));

    @Inject
    Instance<JobRepository> jobRepository;

    @Test
    void provided() throws Exception {
        assertTrue(jobRepository.isResolvable());
        assertInstanceOf(JBeretJpaJobRepository.class, jobRepository.get());
    }
}
