package org.identityconnectors.db2;

import java.sql.*;

import org.identityconnectors.dbcommon.SQLUtil;
import org.junit.*;

public class DB2AuthorityReaderTest {
	
	private static Connection conn;
	private static DB2Configuration config;
	private static DB2AuthorityReader testee;
	
	@BeforeClass
	public static void setup() throws Exception{
		conn = DB2ConnectorTest.createTestConnection();
		config = DB2ConfigurationTest.createTestConfiguration();
		testee = new DB2AuthorityReader(conn);
	}
	
	@AfterClass
	public static void afterClass(){
		SQLUtil.closeQuietly(conn);
	}

	@Test
	public void testReadDatabaseAuthorities() throws SQLException {
		testee.readDatabaseAuthorities(config.getAdminAccount());
	}

	@Test
	public void testReadAllAuthorities() throws SQLException {
		testee.readAllAuthorities(config.getAdminAccount());
	}

	@Test
	public void testReadIndexAuthorities() throws SQLException {
		testee.readIndexAuthorities(config.getAdminAccount());
	}

	@Test
	public void testReadPackageAuthorities() throws SQLException {
		testee.readPackageAuthorities(config.getAdminAccount());
	}

	@Test
	public void testReadSchemaAuthorities() throws SQLException {
		testee.readSchemaAuthorities(config.getAdminAccount());
	}

	@Test
	public void testReadServerAuthorities() throws SQLException {
		testee.readServerAuthorities(config.getAdminAccount());
	}

	@Test
	public void testReadTableAuthorities() throws SQLException {
		testee.readTableAuthorities(config.getAdminAccount());
	}

	@Test
	public void testReadTablespaceAuthorities() throws SQLException {
		testee.readTablespaceAuthorities(config.getAdminAccount());
	}

}
