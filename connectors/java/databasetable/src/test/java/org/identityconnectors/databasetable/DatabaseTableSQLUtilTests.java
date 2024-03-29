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
package org.identityconnectors.databasetable;


import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.Pair;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.databasetable.mapping.DefaultStrategy;
import org.identityconnectors.databasetable.mapping.MappingStrategy;
import org.identityconnectors.dbcommon.ExpectProxy;
import org.identityconnectors.dbcommon.SQLParam;


/**
 * The SQL util tests
 * @version $Revision 1.0$
 * @since 1.0
 */
public class DatabaseTableSQLUtilTests {

    /**
     * GetAttributeSet test method
     * @throws SQLException 
     */
    @Test
    public void testGetColumnValues() throws SQLException {
        final String TEST1 = "test1";
        final String TEST_VAL1 = "testValue1";
        final String TEST2 = "test2";
        final String TEST_VAL2 = "testValue2";

        //Resultset
        final ExpectProxy<ResultSet> trs = new ExpectProxy<ResultSet>();
        ResultSet resultSetProxy = trs.getProxy(ResultSet.class);
        
        //Metadata
        final ExpectProxy<ResultSetMetaData> trsmd = new ExpectProxy<ResultSetMetaData>();
        ResultSetMetaData metaDataProxy = trsmd.getProxy(ResultSetMetaData.class);

        trs.expectAndReturn("getMetaData",metaDataProxy);
        trsmd.expectAndReturn("getColumnCount", 2);
        trsmd.expectAndReturn("getColumnName", TEST1);
        trsmd.expectAndReturn("getColumnType", Types.VARCHAR);
        trsmd.expectAndReturn("getColumnTypeName", "varchar");
        trs.expectAndReturn("getString", TEST_VAL1);
        trsmd.expectAndReturn("getColumnName", TEST2);        
        trsmd.expectAndReturn("getColumnType", Types.VARCHAR);
        trsmd.expectAndReturn("getColumnTypeName", "varchar");
        trs.expectAndReturn("getString", TEST_VAL2);
        
        final DefaultStrategy derbyDbStrategy = new DefaultStrategy();
         Map<String, SQLParam> actual = null;
         actual = DatabaseTableSQLUtil.getColumnValues(derbyDbStrategy, resultSetProxy);
        assertTrue("getString not called", trs.isDone());
        assertTrue("getColumnType not called", trsmd.isDone());
        assertEquals(2, actual.size());
        final SQLParam tv1 =  actual.get(TEST1);
        AssertJUnit.assertNotNull(tv1);
        assertEquals(TEST_VAL1, tv1.getValue()); 
        final SQLParam tv2 =  actual.get(TEST2);
        AssertJUnit.assertNotNull(tv2);
        assertEquals(TEST_VAL2, tv2.getValue()); 
     }   
    
    /**
     * Test quoting method
     * @throws Exception
     */
    @Test
    public void testQuoting() throws Exception {
        final Map<String, Pair<String, String>> data = new HashMap<String, Pair<String, String>>();
        data.put("none", new Pair<String, String>("afklj", "afklj"));
        data.put("double", new Pair<String, String>("123jd", "\"123jd\""));
        data.put("single", new Pair<String, String>("di3nfd", "'di3nfd'"));
        data.put("back", new Pair<String, String>("fadfk3", "`fadfk3`"));
        data.put("brackets", new Pair<String, String>("fadlkfj", "[fadlkfj]"));
        for (Map.Entry<String, Pair<String, String>> entry : data.entrySet()) {
            final String actual = DatabaseTableSQLUtil.quoteName(entry.getKey(), entry.getValue().first);
            assertEquals(entry.getValue().second, actual);
        }
    }
    
    /**
     * Test quoting method
     * @throws Exception
     */
    @Test
    public void testSetParams() throws Exception {
        final ExpectProxy<MappingStrategy> mse = new ExpectProxy<MappingStrategy>();
        MappingStrategy ms = mse.getProxy(MappingStrategy.class);
        
        final ExpectProxy<PreparedStatement> pse = new ExpectProxy<PreparedStatement>();
        PreparedStatement ps = pse.getProxy(PreparedStatement.class);        
        
        final ExpectProxy<CallableStatement> cse = new ExpectProxy<CallableStatement>();
        CallableStatement cs = cse.getProxy(CallableStatement.class);        
        
        List<SQLParam> params = new ArrayList<SQLParam>();
        params.add(new SQLParam("test", "test", Types.VARCHAR));
        params.add(new SQLParam("password", new GuardedString("tst".toCharArray()), Types.VARCHAR));
        mse.expect("setSQLParam");
        mse.expect("setSQLParam");
        
        DatabaseTableSQLUtil.setParams(ms, ps, params );
        assertTrue("setSQLParam not called", mse.isDone());
        assertTrue("setSQLParam not called", pse.isDone());

        mse.expect("setSQLParam");       
        mse.expect("setSQLParam");
        DatabaseTableSQLUtil.setParams(ms, cs, params);
        assertTrue("setSQLParam not called", mse.isDone());
        assertTrue("setSQLParam not called", cse.isDone());
    }        
}
