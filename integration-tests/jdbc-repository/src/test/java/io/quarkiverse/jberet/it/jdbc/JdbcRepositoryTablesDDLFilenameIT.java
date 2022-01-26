package io.quarkiverse.jberet.it.jdbc;

import io.quarkus.test.junit.NativeImageTest;
import io.quarkus.test.junit.TestProfile;

@NativeImageTest
@TestProfile(JdbcRepositoryTablesDDLFilenameTest.Profile.class)
public class JdbcRepositoryTablesDDLFilenameIT extends JdbcRepositoryTablesDDLFilenameTest {
}
