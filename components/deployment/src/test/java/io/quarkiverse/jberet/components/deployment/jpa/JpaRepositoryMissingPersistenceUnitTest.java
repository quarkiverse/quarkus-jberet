package io.quarkiverse.jberet.components.deployment.jpa;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

class JpaRepositoryMissingPersistenceUnitTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.jberet.repository.type", "jpa")
            .overrideConfigKey("quarkus.jberet.repository.jpa.persistence-unit-name", "other")
            .setExpectedException(ConfigurationException.class);

    @Test
    void fail() {
        Assertions.fail();
    }
}
