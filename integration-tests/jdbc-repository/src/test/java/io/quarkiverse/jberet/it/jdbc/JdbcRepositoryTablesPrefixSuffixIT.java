package io.quarkiverse.jberet.it.jdbc;

import io.quarkus.test.junit.NativeImageTest;
import io.quarkus.test.junit.TestProfile;

@NativeImageTest
@TestProfile(JdbcRepositoryTablesPrefixSuffixTest.Profile.class)
public class JdbcRepositoryTablesPrefixSuffixIT extends JdbcRepositoryTablesPrefixSuffixTest {
}
