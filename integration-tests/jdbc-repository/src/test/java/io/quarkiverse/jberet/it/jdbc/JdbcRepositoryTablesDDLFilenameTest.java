package io.quarkiverse.jberet.it.jdbc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import io.quarkiverse.jberet.it.jdbc.JdbcRepositoryTablesDDLFilenameTest.Profile;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestProfile(Profile.class)
class JdbcRepositoryTablesDDLFilenameTest extends JdbcRepositoryTest
{
	public static class Profile implements QuarkusTestProfile
	{
		@Override
	    public String getConfigProfile()
		{
	        return "test-ddl-filename";
	    }
	}
	
    @Inject
    @io.quarkus.agroal.DataSource("batch")
    DataSource dataSource;

	@Test
	void testTables() throws SQLException 
	{
		final String sql = "SELECT * FROM jb_test_schema.JOB_INSTANCE JI " + 
		"inner join jb_test_schema.JOB_EXECUTION JE on JE.JOBINSTANCEID = JI.JOBINSTANCEID " +
		"inner join jb_test_schema.STEP_EXECUTION SE on SE.JOBEXECUTIONID = JE.JOBEXECUTIONID " + 
		"inner join jb_test_schema.PARTITION_EXECUTION PE on PE.STEPEXECUTIONID = SE.STEPEXECUTIONID";
		try (Connection connection = dataSource.getConnection())
		{
			try (Statement statement = connection.createStatement())
			{
				statement.execute(sql);
			}
		}
		assertTrue(true);
	}
}
