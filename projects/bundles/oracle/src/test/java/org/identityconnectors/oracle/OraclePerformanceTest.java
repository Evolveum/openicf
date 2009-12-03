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

package org.identityconnectors.oracle;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author kitko
 *
 */
public class OraclePerformanceTest extends OracleConnectorAbstractTest{
    private int tables = 1000;
    /** This test was run just to compare normal and batch updates */
    @Test
    @Ignore
    public void testUpdateRoles() throws SQLException{
        Connection conn = testConf.createAdminConnection();
        try{
            createTestTables(conn);
            Uid uid = new Uid("USER1");
            try{
                facade.delete(ObjectClass.ACCOUNT, uid, null);
            }
            catch(UnknownUidException e){}
            Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
            Attribute name = new Name(uid.getUidValue());
            GuardedString password = new GuardedString("hello".toCharArray());
            Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
            Attribute privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME, "create session");
            uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute,privileges), null);
            List<String> newprivileges = new ArrayList<String>(tables);
            for(int i = 0; i < tables;i++){
                newprivileges.add("SELECT ON MYTABLE" + i);
            }
            long start = System.currentTimeMillis();
            facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME, newprivileges)), null);
            long end = System.currentTimeMillis();
            System.out.println((end - start) + " ms");
            facade.delete(ObjectClass.ACCOUNT, uid, null);
        }
        finally{
            dropTestTables(conn);
            conn.close();
        }
    }

    /**
     * @param conn
     * @throws SQLException 
     */
    private void dropTestTables(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        for(int i = 0; i < tables; i++){
            String sql = "drop table mytable" + i;
            st.addBatch(sql);
        }
        
        st.executeBatch();
        st.close();
        conn.commit();
    }

    private void createTestTables(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        for(int i = 0; i < tables; i++){
            String sql = "create table mytable" + i + "(ID Number(1))";
            st.addBatch(sql);
        }
        st.executeBatch();
        st.close();
        conn.commit();
    }
}
