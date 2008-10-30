package org.identityconnectors.db2;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.Collection;

import org.junit.Test;

public class DB2SpecificsTest {
	/** Test loading of exclude names from resource */
	@Test
	public void testLoadExcludeNames(){
		final Collection<String> excludeNames = DB2Specifics.getExcludeNames();
		assertNotNull(excludeNames);
		assertTrue("Exclude names must contain COUNT",excludeNames.contains("COUNT"));
		assertTrue("Exclude names must contain LOCAL",excludeNames.contains("LOCAL"));
		assertTrue("Must contain at least 50 exclude names",excludeNames.size() >= 50);
	}
	
	/**
	 * Here I have manually tested stale connection.
	 * Normally this test pass, just creates connection.
	 * To simulate stale connection, I have killed connection using
	 * "db2 force application(handle)" command.
	 * @throws Exception
	 */
	@Test
	public void testStaleConnection() throws Exception{
		Connection conn = DB2ConnectorTest.createTestConnection();
		DB2Specifics.testConnection(conn);
		conn.commit();
		conn.close();
	}
	

}
