package org.identityconnectors.db2;

import java.sql.Connection;

import org.junit.Test;

public class DB2ConnectionTest {
	
	/**
	 * Here I have manually tested stale connection.
	 * Normally this test pass, just creates connection.
	 * To simulate stale connection, I have killed connection using
	 * "db2 force application(handle)" command.
	 * @throws Exception
	 */
	@Test
	public void testStaleConnection() throws Exception{
		Connection conn = createTestConnection();
		DB2Connection db2Connection = new DB2Connection(conn);
		db2Connection.test();
		db2Connection.dispose();
	}
	
	private Connection createTestConnection() throws Exception{
		DB2Configuration conf = DB2ConfigurationTest.createTestConfiguration();
		return DB2Connection.createDB2Connection(DB2Connection.APP_DRIVER,
				conf.getHost(), conf.getPort(), "db2", conf.getDatabaseName(),conf.getAdminAccount(),
				conf.getAdminPassword());
	}
	

}
