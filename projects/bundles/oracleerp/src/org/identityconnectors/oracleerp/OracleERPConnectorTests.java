/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.oracleerp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Attempts to test the {@link OracleERPConnector} with the framework.
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class OracleERPConnectorTests { 


    // Test facade
    private ConnectorFacade facade = null;

    private OracleERPConfiguration config;
    
    /*
     * Connector test properties. On your computer, these are defined in: 
     * "user.home".connector-oracleerp.properties. 
     */
    private static final String CON_URL = TestHelpers.getProperty("connectionUrl.connector.string", null);
    private static final String LOGIN = TestHelpers.getProperty("LOGIN", null);
    private static final String PASSWORD = TestHelpers.getProperty("PASSWORD", null);

    String idmHost = TestHelpers.getProperty("connectionUrl.connector.string", null);
    String idmLogin = TestHelpers.getProperty("login.connector.string", null);
    String idmPassword = TestHelpers.getProperty("password.connector.string", null);
    String idmPort = TestHelpers.getProperty("port.connector.string", null);
    String idmDriver = TestHelpers.getProperty("driver.connector.string", null);     
    
    //set up logging
    private static final Log log = Log.getLog(OracleERPConnectorTests.class);
    
    @BeforeClass
    public static void setUp() { 
        //Assert.assertNotNull(CON_URL);
        //Assert.assertNotNull(LOGIN);
        //Assert.assertNotNull(PASSWORD);
         
        //
        //other setup work to do before running tests
        //
    }
    
    @AfterClass
    public static void tearDown() {
        //
        //clean up resources
        //
    }

    /**
     * Setup  the test
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        // attempt to create the database in the directory..
        config = newConfiguration();
        facade = getFacade();
    }
    
    /**
     * @return
     */
    private OracleERPConfiguration newConfiguration() {
        config=new OracleERPConfiguration();
        config.setUser("APPL");
        return config;
    }

    /**
     * @return
     */
    private ConnectorFacade getFacade() {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * Test method for {@link OracleERPConnector#schema()}.
     */
    //Test
    public void testConfig() {
        Schema schema = facade.schema();
        // Schema should not be null
        assertNotNull(schema);
        Set<ObjectClassInfo> objectInfos = schema.getObjectClassInfo();
        assertNotNull(objectInfos);
        assertEquals(1, objectInfos.size());
        ObjectClassInfo objectInfo = (ObjectClassInfo) objectInfos.toArray()[0];
        assertNotNull(objectInfo);
        // the object class has to ACCOUNT_NAME
        assertEquals(ObjectClass.ACCOUNT_NAME, objectInfo.getType());
        // iterate through AttributeInfo Set
        Set<AttributeInfo> attInfos = objectInfo.getAttributeInfo();
        
        assertNotNull(AttributeInfoUtil.find(Name.NAME, attInfos));
        assertNotNull(AttributeInfoUtil.find(OperationalAttributes.PASSWORD_NAME, attInfos));
    }
    
    
    @Test
    public void testCreateUserAccount() {
        final Set<Attribute> attrs = createAllAttributes();
        final Map<String, Object> userValues =  OracleERPConnector.Account.getUserValuesMap(ObjectClass.ACCOUNT, attrs, null, true);

        //test sql
        Assert.assertEquals("Invalid SQL",
                        "{ call APPL.fnd_user_pkg.CreateUser ( x_user_name => ?, x_owner => upper(?), "+
                        "x_unencrypted_password => ?, x_start_date => ?, x_end_date => ?, "+
                        "x_last_logon_date => ?, x_description => ?, x_password_date => ?, "+
                        "x_password_accesses_left => ?, x_password_lifespan_accesses => ?, "+
                        "x_password_lifespan_days => ?, x_employee_id => ?, x_email_address => ?, "+
                        "x_fax => ?, x_customer_id => ?, x_supplier_id => ? ) }",
                        OracleERPConnector.Account.getUserCallSQL(userValues, true, config.getSchemaId()));
        //session is always null, so 16 params
        Assert.assertEquals("Invalid number of  SQL Params", 16, OracleERPConnector.Account.getSQLParams(userValues).size());
        
        //Old style of creating user
        Assert.assertEquals("Invalid All SQL",
                "{ call APPL.fnd_user_pkg.CreateUser ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) }",
                OracleERPConnector.Account.getAllSQL(userValues, true, config.getSchemaId()));
        
        //all 17 params
        Assert.assertEquals("Invalid number of  SQL Params", 17, OracleERPConnector.Account.getAllSQLParams(userValues).size());

        Assert.assertFalse("Is update needed", OracleERPConnector.Account.isUpdateNeeded(userValues));
    }

    /**
     * @return
     */
    private Set<Attribute> createAllAttributes() {
        Set<Attribute> attrs = new HashSet<Attribute>();
        attrs.add(new Name("test_name"));
        attrs.add(AttributeBuilder.buildPasswordExpired(false));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.OWNER, "test_owner"));
        attrs.add(AttributeBuilder.buildPassword("test_pwd".toCharArray()));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.OWNER, "test_owner"));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.START_DATE, (new Timestamp(System.currentTimeMillis())).toString()));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.DESCR, "descrip"));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.END_DATE, (new Timestamp(System.currentTimeMillis())).toString()));        
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.PWD_ACCESSES_LEFT, 54));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.PWD_LIFE_ACCESSES, 55));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.PWD_LIFE_DAYS, 56));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.EMP_ID, 101));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.EMAIL, "test@google.com"));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.FAX, "12456"));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.CUST_ID, 120));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.SUPP_ID, 130));
        return attrs;
    }

    /**
     * @return
     */
    private Set<Attribute> createNullAttributes() {
        Set<Attribute> attrs = new HashSet<Attribute>();
        attrs.add(new Name("test_name"));
        attrs.add(AttributeBuilder.buildPasswordExpired(false));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.OWNER, "test_owner"));
        attrs.add(AttributeBuilder.buildPassword("test_pwd".toCharArray()));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.OWNER, "test_owner"));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.START_DATE, (String) null));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.DESCR, (String) null));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.END_DATE, (String) null));        
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.PWD_ACCESSES_LEFT, (String) null));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.PWD_LIFE_ACCESSES, (String) null));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.PWD_LIFE_DAYS, (String) null));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.EMP_ID, (String) null));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.EMAIL, (String) null));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.FAX, (String) null));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.CUST_ID, (String) null));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.SUPP_ID, (String) null));
        return attrs;
    }    
    
    @Test
    public void testCreateUserAccountNulls() {
        final Set<Attribute> attrs = createNullAttributes();
        final Map<String, Object> userValues =  OracleERPConnector.Account.getUserValuesMap(ObjectClass.ACCOUNT, attrs, null, true);
        //test sql
        Assert.assertEquals("Invalid SQL",
                "{ call APPL.fnd_user_pkg.CreateUser ( x_user_name => ?, x_owner => upper(?), "+
                "x_unencrypted_password => ?, x_end_date => FND_USER_PKG.null_date, "+
                "x_last_logon_date => ?, x_description => FND_USER_PKG.null_char, "+
                "x_password_date => ?, x_password_accesses_left => FND_USER_PKG.null_number, "+
                "x_password_lifespan_accesses => FND_USER_PKG.null_number, "+
                "x_password_lifespan_days => FND_USER_PKG.null_number, x_employee_id => FND_USER_PKG.null_number, "+
                "x_email_address => FND_USER_PKG.null_char, x_fax => FND_USER_PKG.null_char, "+
                "x_customer_id => FND_USER_PKG.null_number, x_supplier_id => FND_USER_PKG.null_number ) }",
                        OracleERPConnector.Account.getUserCallSQL(userValues, true, config.getSchemaId()));
        //session is always null, so 16 params
        Assert.assertEquals("Invalid number of  SQL Params", 5, OracleERPConnector.Account.getSQLParams(userValues).size());
        
        //Test Old style of creating user
        Assert.assertEquals("Invalid All SQL",
                "{ call APPL.fnd_user_pkg.CreateUser ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) }",
                OracleERPConnector.Account.getAllSQL(userValues, true, config.getSchemaId()));
        
        //all 17 params
        Assert.assertEquals("Invalid number of  SQL Params", 17, OracleERPConnector.Account.getAllSQLParams(userValues).size());

        Assert.assertTrue("Is update needed", OracleERPConnector.Account.isUpdateNeeded(userValues));

        Assert.assertEquals("Invalid All SQL",
                "{ call APPL.fnd_user_pkg.UpdateUser ( x_user_name => ?, x_owner => upper(?), "+
                "x_end_date => FND_USER_PKG.null_date, "+
                "x_description => FND_USER_PKG.null_char, x_password_accesses_left => FND_USER_PKG.null_number, "+
                "x_password_lifespan_accesses => FND_USER_PKG.null_number, "+
                "x_password_lifespan_days => FND_USER_PKG.null_number, x_employee_id => FND_USER_PKG.null_number, "+
                "x_email_address => FND_USER_PKG.null_char, x_fax => FND_USER_PKG.null_char, "+
                "x_customer_id => FND_USER_PKG.null_number, x_supplier_id => FND_USER_PKG.null_number ) }",
                OracleERPConnector.Account.getUpdateNullsSQL(userValues, config.getSchemaId()));
        
        //the required user name and owner
        Assert.assertEquals("Invalid number of update SQL Params", 2, OracleERPConnector.Account.getUpdateNullsParams(userValues).size());        
    }        

}
