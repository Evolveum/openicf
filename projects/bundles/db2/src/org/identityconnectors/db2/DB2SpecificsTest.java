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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.Collection;

import org.junit.Test;

/**
 * Test DB2 specifics
 * @author kitko
 *
 */
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
		DB2Specifics.testConnection(conn,DB2Specifics.findTestSQL(conn));
		conn.commit();
		conn.close();
	}
	

}
