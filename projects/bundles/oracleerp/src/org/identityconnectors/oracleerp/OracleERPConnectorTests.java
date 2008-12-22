/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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
package org.identityconnectors.oracleerp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.contract.data.DataProvider;
import org.identityconnectors.contract.test.ConnectorHelper;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoUtil;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.After;
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

    static final String DEFAULT_CONFIGURATINON = "configuration.init";
    static final String ACCOUNT_ALL_ATTRS = "account.all";
    static final String ACCOUNT_REQUIRED_ATTRS = "account.required";
    static final String ACCOUNT_MODIFY_ATTRS = "account.modify";
    static final String NEW_USER_ATTRS = "account.required";


    // Test facade
    private ConnectorFacade facade = null;

    private OracleERPConfiguration config;

    
    /*
     * Connector test properties. On your computer, these are defined in: 
     * "user.home"/.connector/connecotr-oracleerp/build.groovy          ... -Dproject.name=connector-oracleerp
     * "user.home"/.connector/connecotr-oracleerp/11_5_9/build.groovy   ... -Dconfiguration=11_5_9
     * "user.home"/.connector/connecotr-oracleerp/11_5_10/build.groovy  ... -Dconfiguration=11_5_10
     * "user.home"/.connector/connecotr-oracleerp/12_0_2/build.groovy   ... -Dconfiguration=12
     */  
    
    //set up logging
    private static final Log log = Log.getLog(OracleERPConnectorTests.class);


    
    @BeforeClass
    public static void setUpClass() { 
    }


    /**
     * Setup  the test
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        // attempt to create the database in the directory..
        config = new OracleERPConfiguration();
        loadConfiguration(DEFAULT_CONFIGURATINON, config); 
        assertNotNull(config);
        facade = getFacade(config);
        assertNotNull(facade);
    }
    
    /**
     * Tear down class
     */
    @After
    public void tearDown() {
        config=null;
        facade=null;
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


    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConfigurationProperties() {
        assertNotNull(config.getDriver());
        assertNotNull(config.getPort());
        assertNotNull(config.getDatabaseName());
        assertNotNull(config.getHostName());
        assertNotNull(config.getUser());
        assertNotNull(config.getPassword());
        assertFalse(config.isAccountsIncluded());
        assertFalse(config.isActiveAccountsOnly());
        assertNotNull(config.getAuditResponsibility());
        assertTrue(config.isManageSecuringAttrs());
        assertFalse(config.isNoSchemaId());
        assertFalse(config.isReturnSobOrgAttrs());
        assertNotNull(config.getUserActions());        
        assertNotNull(config.getConnectionUrl());
    }
    
    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConnectorTest() {
        config.validate();
        ConnectorFacade conn = getFacade(config);
        conn.test();
    }


    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    //@Test
    public void testCreate() {
        assertNotNull(facade);
        final Set<Attribute> attrs = createAllAccountAttributes();
        quitellyDeleteUser(AttributeUtil.getNameFromAttributes(attrs).getName());
        final Uid uid = facade.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
        //assertEquals(tstName, uid.getUidValue());
        //Delete it at the end
        quitellyDeleteUser(uid.getUidValue());
    }    
    

    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    //@Test
    public void testUpdate() {
        assertNotNull(facade);
        final Set<Attribute> attrs = createAllAccountAttributes();
        final Uid uid = facade.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
        //assertEquals(tstName, uid.getUidValue());
        //Delete it at the end
        quitellyDeleteUser(uid.getUidValue());
    }    
    

    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    //@Test
    public void testDelete() {
        assertNotNull(facade);
        final Set<Attribute> attrs = createAllAccountAttributes();
        final Uid uid = facade.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
    }    
    

    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test
    public void testCreateNullDefaults() {
        assertNotNull(facade);
        OracleERPConnector c = new OracleERPConnector(); 
        c.init(this.config);        
        final Set<Attribute> attrs = createNullAccountAttributes();
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
        
        List<ConnectorObject> results = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        assertTrue("expect 1 connector object", results.size() == 1);
 //       assertEquals(tstName, uid.getUidValue());
        //Delete it at the end
        quitellyDeleteUser(uid);
    }
    
    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     * @throws NoSuchFieldException 
     * @throws SecurityException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    @Test
    public void testDefaultConfiguration() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        assertNotNull(config.getDriver());
        assertNotNull(config.getPort());
        assertNotNull(config.getDatabaseName());
        assertNotNull(config.getHostName());
        assertNotNull(config.getUser());
        assertNotNull(config.getPassword());
        assertFalse(config.isAccountsIncluded());
        assertFalse(config.isActiveAccountsOnly());
        assertNotNull(config.getAuditResponsibility());
        assertTrue(config.isManageSecuringAttrs());
        assertFalse(config.isNoSchemaId());
        assertFalse(config.isReturnSobOrgAttrs());
        assertNotNull(config.getUserActions());        
        assertNotNull(config.getConnectionUrl());        
    }    

    @Test
    public void testUserCallSQL() {
        final Set<Attribute> attrs = createAllAccountAttributes();
        OracleERPConnector cn = new OracleERPConnector();
        final Map<String, Object> userValues =  cn.getUserValuesMap(ObjectClass.ACCOUNT, attrs, null, true);

        //test sql
        Assert.assertEquals("Invalid SQL",
                        "{ call APPS.fnd_user_pkg.CreateUser ( x_user_name => ?, x_owner => upper(?), "+
                        "x_unencrypted_password => ?, x_start_date => ?, x_end_date => ?, "+
                        "x_last_logon_date => ?, x_description => ?, x_password_date => ?, "+
                        "x_password_accesses_left => ?, x_password_lifespan_accesses => ?, "+
                        "x_password_lifespan_days => ?, x_employee_id => ?, x_email_address => ?, "+
                        "x_fax => ?, x_customer_id => ?, x_supplier_id => ? ) }",
                        cn.getUserCallSQL(userValues, true, config.getSchemaId()));
        //session is always null, so 16 params
        Assert.assertEquals("Invalid number of  SQL Params", 16, cn.getUserSQLParams(userValues).size());
        
        //Old style of creating user
        Assert.assertEquals("Invalid All SQL",
                "{ call APPS.fnd_user_pkg.CreateUser ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) }",
                cn.getAllSQL(userValues, true, config.getSchemaId()));
        
        //all 17 params
        Assert.assertEquals("Invalid number of  SQL Params", 17, cn.getAllSQLParams(userValues).size());

        Assert.assertFalse("Is update needed", cn.isUpdateNeeded(userValues));
    }    

    @Test
    public void testUserCallSQLNulls() {
        final Set<Attribute> attrs = createNullAccountAttributes();
        OracleERPConnector cn = new OracleERPConnector();
        final Map<String, Object> userValues =  cn.getUserValuesMap(ObjectClass.ACCOUNT, attrs, null, true);
        //test sql
        Assert.assertEquals("Invalid SQL",
                "{ call APPS.fnd_user_pkg.CreateUser ( x_user_name => ?, x_owner => upper(?), "+
                "x_unencrypted_password => ?, x_end_date => FND_USER_PKG.null_date, "+
                "x_last_logon_date => ?, x_description => FND_USER_PKG.null_char, "+
                "x_password_date => ?, x_password_accesses_left => FND_USER_PKG.null_number, "+
                "x_password_lifespan_accesses => FND_USER_PKG.null_number, "+
                "x_password_lifespan_days => FND_USER_PKG.null_number, x_employee_id => FND_USER_PKG.null_number, "+
                "x_email_address => FND_USER_PKG.null_char, x_fax => FND_USER_PKG.null_char, "+
                "x_customer_id => FND_USER_PKG.null_number, x_supplier_id => FND_USER_PKG.null_number ) }",
                cn.getUserCallSQL(userValues, true, config.getSchemaId()));
        //session is always null, so 16 params
        Assert.assertEquals("Invalid number of  SQL Params", 5, cn.getUserSQLParams(userValues).size());
        
        //Test Old style of creating user
        Assert.assertEquals("Invalid All SQL",
                "{ call APPS.fnd_user_pkg.CreateUser ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) }",
                cn.getAllSQL(userValues, true, config.getSchemaId()));
        
        //all 17 params
        Assert.assertEquals("Invalid number of  SQL Params", 17, cn.getAllSQLParams(userValues).size());

        Assert.assertTrue("Is update needed", cn.isUpdateNeeded(userValues));

        Assert.assertEquals("Invalid All SQL",
                "{ call APPS.fnd_user_pkg.UpdateUser ( x_user_name => ?, x_owner => upper(?), "+
                "x_end_date => FND_USER_PKG.null_date, "+
                "x_description => FND_USER_PKG.null_char, x_password_accesses_left => FND_USER_PKG.null_number, "+
                "x_password_lifespan_accesses => FND_USER_PKG.null_number, "+
                "x_password_lifespan_days => FND_USER_PKG.null_number, x_employee_id => FND_USER_PKG.null_number, "+
                "x_email_address => FND_USER_PKG.null_char, x_fax => FND_USER_PKG.null_char, "+
                "x_customer_id => FND_USER_PKG.null_number, x_supplier_id => FND_USER_PKG.null_number ) }",
                cn.getUserUpdateNullsSQL(userValues, config.getSchemaId()));
        
        //the required user name and owner
        Assert.assertEquals("Invalid number of update SQL Params", 2, cn.getUserUpdateNullsParams(userValues).size());        
    }

    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testValidateConfiguration() {
        config.validate();
    }

    /**
     * @return
     */
    private Set<Attribute> createAllAccountAttributes() {
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_ALL_ATTRS);       
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.START_DATE, (new Timestamp(System.currentTimeMillis()-10*24*3600000)).toString()));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.END_DATE, (new Timestamp(System.currentTimeMillis()+10*24*3600000)).toString()));        
        
        /*        attrs.add(AttributeBuilder.buildPasswordExpired(false));                
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.DESCR, "descrip"));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.PWD_ACCESSES_LEFT, 54));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.PWD_LIFE_ACCESSES, 55));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.PWD_LIFE_DAYS, 56));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.EMP_ID, 101));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.EMAIL, "test@google.com"));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.FAX, "12456"));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.CUST_ID, 120));
        attrs.add(AttributeBuilder.build(OracleERPConnector.Account.SUPP_ID, 130));*/
        return attrs;
    }
    
    /**
     * @return
     */
    private Set<Attribute> createNullAccountAttributes() {
        Set<Attribute> attrs = createRequiredAccountAttributes();
        attrs.add(AttributeBuilder.buildPasswordExpired(false));        
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

    
    /**
     * @return
     */
    private Set<Attribute> createRequiredAccountAttributes() {
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);       
        return attrs;
    }
    
    
    /**
     * @param userName
     * @param testPassword2
     * @return
     */
    private Uid createUser() {
        Set<Attribute> tuas = getAttributeSet(NEW_USER_ATTRS);
        assertNotNull(tuas);
        return facade.create(ObjectClass.ACCOUNT, tuas, null);
    }     
    

    /**
     * @param  propertySetName
     * @return The set <CODE>Set<Attribute></CODE> of attributes 
     */
    private Set<Attribute> getAttributeSet(final String propertySetName) {
        Map<String, Object> propMap = getPropertyMap(propertySetName);   
        assertNotNull(propMap);
        Set<Attribute> attrSet = new  LinkedHashSet<Attribute>();
        for (Entry<String, Object> entry : propMap.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            
            if(Uid.NAME.equals(key)) {
                attrSet.add(new Uid(value.toString()));
            } else if (OperationalAttributes.PASSWORD_NAME.equals(key)) {
                attrSet.add(AttributeBuilder.buildPassword(value.toString().toCharArray()));
            } else if (OperationalAttributes.CURRENT_PASSWORD_NAME.equals(key)) {
                attrSet.add(AttributeBuilder.buildCurrentPassword(value.toString().toCharArray()));
            } else if (OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME.equals(key)) {
                attrSet.add(AttributeBuilder.buildPasswordExpirationDate((Long) value));
            } else if (OperationalAttributes.PASSWORD_EXPIRED_NAME.equals(key)) {
                attrSet.add(AttributeBuilder.buildPasswordExpired((Boolean) value));
            } else if (OperationalAttributes.DISABLE_DATE_NAME.equals(key)) {
                attrSet.add(AttributeBuilder.buildDisableDate((Long) value));
            } else if (OperationalAttributes.ENABLE_DATE_NAME.equals(key)) {
                attrSet.add(AttributeBuilder.buildEnableDate((Long) value));
            } else if (OperationalAttributes.ENABLE_NAME.equals(key)) {
                attrSet.add(AttributeBuilder.buildEnabled((Boolean) value));
            } else if (OperationalAttributes.LOCK_OUT_NAME.equals(key)) {
                attrSet.add(AttributeBuilder.buildLockOut((Boolean) value));
            } else {
                attrSet.add(AttributeBuilder.build(key, value));
            }
        }
        return attrSet;
    }    
    
    
    /**
     * @return
     */
    private ConnectorFacade getFacade(Configuration config) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        // **test only**
        APIConfiguration impl = TestHelpers.createTestConfiguration(OracleERPConnector.class, config);
        return factory.newInstance(impl);
    }


    /**
     * @param setName
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getPropertyMap(final String setName) {
        DataProvider dataProvider = ConnectorHelper.createDataProvider();        
        Map<String, Object> propMap = (Map<String, Object>) dataProvider.get(setName);
        return propMap;
    }    
    
    /**
     * @param configName
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void loadConfiguration(final String configName, Configuration cfg)    
            throws NoSuchFieldException, IllegalAccessException {
        Map<String, Object> propMap = getPropertyMap(configName);   
        assertNotNull(propMap);       
        for (Entry<String, Object> entry : propMap.entrySet()) {
            final String key = entry.getKey();
            final Field fld = cfg.getClass().getDeclaredField(key);
            final Object value = entry.getValue();
            fld.setAccessible(true);
            final Class<?> type = fld.getType();
            if(type.isAssignableFrom(GuardedString.class)) {
                fld.set(cfg, new GuardedString(value.toString().toCharArray()));
            } else {
                fld.set(cfg, value);
            }
        }
    }

    /**
     * @param userName
     */
    private void quitellyDeleteUser(String name) {
        quitellyDeleteUser(new Uid(name));
    }     
    
    /**
     * @param userName
     */
    private void quitellyDeleteUser(Uid uid) {
        try{
            facade.delete(ObjectClass.ACCOUNT, uid, null);
        } catch (Exception ex) {
            log.error(ex, "expected");
            // handle exception
        }         
    }      
}
