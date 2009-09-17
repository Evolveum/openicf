/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.db2;

import java.sql.*;

import org.identityconnectors.dbcommon.*;
import org.identityconnectors.test.common.*;
import org.junit.*;

/** Test of DB2AuthorityReader 
 * All of tests are just smoke test.
 * TODO : For each reading try to revoke,grant and test that reading is correct
 * */
public class DB2AuthorityReaderTest {
	
	private static Connection conn;
	private static DB2AuthorityReader testee;
	private static String testUser;
	
	
	/**
	 * Setup
	 * @throws Exception
	 */
	@BeforeClass
	public static void setup() throws Exception{
		conn = DB2ConnectorTest.createTestConnection();
		testee = new DB2AuthorityReader(conn);
		testUser = TestHelpers.getProperties(DB2Connector.class).getStringProperty("testUser");
	}
	
	/**
	 * TearDown
	 */
	@AfterClass
	public static void afterClass(){
		SQLUtil.closeQuietly(conn);
	}

	/**
	 * Test reading database authorities
	 * @throws SQLException
	 */
	@Test
	public void testReadDatabaseAuthorities() throws SQLException {
		testee.readDatabaseAuthorities(testUser);
	}

	/**
	 * Test reading all authorities
	 * @throws SQLException
	 */
	@Test
	public void testReadAllAuthorities() throws SQLException {
		testee.readAllAuthorities(testUser);
	}

	/**
	 * Test reading index authorities
	 * @throws SQLException
	 */
	@Test
	public void testReadIndexAuthorities() throws SQLException {
		testee.readIndexAuthorities(testUser);
	}

	/**
	 * Test reading package authorities
	 * @throws SQLException
	 */
	@Test
	public void testReadPackageAuthorities() throws SQLException {
		testee.readPackageAuthorities(testUser);
	}

	/**
	 * Test reading schema authorities
	 * @throws SQLException
	 */
	@Test
	public void testReadSchemaAuthorities() throws SQLException {
		testee.readSchemaAuthorities(testUser);
	}

	/**
	 * Test reading server authorities
	 * @throws SQLException
	 */
	@Test
	public void testReadServerAuthorities() throws SQLException {
		testee.readServerAuthorities(testUser);
	}

	/**
	 * Test reading table authorities
	 * @throws SQLException
	 */
	@Test
	public void testReadTableAuthorities() throws SQLException {
		testee.readTableAuthorities(testUser);
	}

	/**
	 * Test reading tablespace authorities
	 * @throws SQLException
	 */
	@Test
	public void testReadTablespaceAuthorities() throws SQLException {
		testee.readTablespaceAuthorities(testUser);
	}

}
