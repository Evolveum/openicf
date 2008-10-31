package org.identityconnectors.db2;

import java.sql.*;
import java.util.Collection;

import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.*;

public class DB2AuthorityReaderTest {
	
	private static Connection conn;
	private static DB2AuthorityReader testee;
	private static String testUser;
	
	@BeforeClass
	public static void setup() throws Exception{
		conn = DB2ConnectorTest.createTestConnection();
		testee = new DB2AuthorityReader(conn);
		testUser = TestHelpers.getProperty("testUser",null);
	}
	
	@AfterClass
	public static void afterClass(){
		SQLUtil.closeQuietly(conn);
	}
	
	private void testAuthoritiesNotEmpty(final Collection<DB2Authority> authorities){
		Assert.assertTrue(!authorities.isEmpty());
	}

	@Test
	public void testReadDatabaseAuthorities() throws SQLException {
		testee.readDatabaseAuthorities(testUser);
	}

	@Test
	public void testReadAllAuthorities() throws SQLException {
		testAuthoritiesNotEmpty(testee.readAllAuthorities(testUser));
	}

	@Test
	public void testReadIndexAuthorities() throws SQLException {
		testAuthoritiesNotEmpty(testee.readIndexAuthorities(testUser));
	}

	@Test
	public void testReadPackageAuthorities() throws SQLException {
		testAuthoritiesNotEmpty(testee.readPackageAuthorities(testUser));
	}

	@Test
	public void testReadSchemaAuthorities() throws SQLException {
		testee.readSchemaAuthorities(testUser);
	}

	@Test
	public void testReadServerAuthorities() throws SQLException {
		testee.readServerAuthorities(testUser);
	}

	@Test
	public void testReadTableAuthorities() throws SQLException {
		testAuthoritiesNotEmpty(testee.readTableAuthorities(testUser));
	}

	@Test
	public void testReadTablespaceAuthorities() throws SQLException {
		testAuthoritiesNotEmpty(testee.readTablespaceAuthorities(testUser));
	}

}
