package io.quarkiverse.jberet.it.joblauncher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
class BatchApplicationTest {
    @Test
    @Launch
    void main(LaunchResult result) {
        assertEquals(0, result.exitCode());
    }

    @Test
    @Launch("restart")
    void restart(LaunchResult result) {
        assertEquals(0, result.exitCode());
    }
}
