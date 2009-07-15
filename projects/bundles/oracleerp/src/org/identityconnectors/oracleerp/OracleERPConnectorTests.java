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
package org.identityconnectors.oracleerp;

import static org.identityconnectors.common.StringUtil.isBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.contract.data.DataProvider;
import org.identityconnectors.contract.test.ConnectorHelper;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.Attribute;
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
import org.identityconnectors.test.common.TestHelpers;
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
    
    static  DataProvider dataProvider = null; 
    
    // Test facade
    private ConnectorFacade facade = null;

    private OracleERPConfiguration config;
    
    //set up logging
    private static final Log log = Log.getLog(OracleERPConnectorTests.class);


    
    /**
     * The class load method
     */
    @BeforeClass
    public static void setUpClass() { 
        dataProvider = ConnectorHelper.createDataProvider();
    }


    /**
     * Setup  the test
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        // attempt to create the database in the directory..
        config = new OracleERPConfiguration();        
        dataProvider.loadConfiguration(DEFAULT_CONFIGURATINON, config); 
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
        assertTrue(objectInfo.is(ObjectClass.ACCOUNT_NAME));
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
        if(isBlank(config.getUrl())) {
            assertNotNull(config.getHost());
            assertNotNull(config.getUser());
            assertNotNull(config.getPort());
        }
        assertNotNull(config.getPassword());
        assertNotNull(config.getAccountsIncluded());
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
    @Test
    public void testCreateRequiredOnly() {
        assertNotNull(facade);
        OracleERPConnector c = new OracleERPConnector(); 
        c.init(this.config);        
        final Set<Attribute> attrs = dataProvider.getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
        
        List<ConnectorObject> results = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        assertTrue("expect 1 connector object", results.size() == 1);
        System.out.println(results.get(0).getAttributes());
    }
    
    
    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test
    public void testCreate() {
        assertNotNull(facade);
        final Set<Attribute> attrs = dataProvider.getAttributeSet(ACCOUNT_ALL_ATTRS);
        
        OracleERPConnector c = new OracleERPConnector(); 
        c.init(this.config);         
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);

        List<ConnectorObject> results = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        assertTrue("expect 1 connector object", results.size() == 1);
        System.out.println(results.get(0).getAttributes());
    }    
    

    
    
    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    //@Test
    public void testUpdate() {
        assertNotNull(facade);
        final Set<Attribute> attrs = dataProvider.getAttributeSet(ACCOUNT_ALL_ATTRS);
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
        final Set<Attribute> attrs = dataProvider.getAttributeSet(ACCOUNT_ALL_ATTRS);
        final Uid uid = facade.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
    }    
 
    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testValidateConfiguration() {
        config.validate();
    }

    /**
     * @param userName
     * @param testPassword2
     * @return
     */
    private Uid createUser() {
        Set<Attribute> tuas = dataProvider.getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        assertNotNull(tuas);
        return facade.create(ObjectClass.ACCOUNT, tuas, null);
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
    
    
    /**
     * @param expected
     * @param actual
     */
    protected void attributeSetsEquals(final Schema schema, Set<Attribute> expected, Set<Attribute> actual, String ... ignore) {
        attributeSetsEquals(schema, AttributeUtil.toMap(expected), AttributeUtil.toMap(actual), ignore);              
    }    
    
     /**
     * @param expected
     * @param actual
     */
    protected void attributeSetsEquals(final Schema schema, final Map<String, Attribute> expMap, final Map<String, Attribute> actMap, String ... ignore) {
        log.ok("attributeSetsEquals");
        final Set<String> ignoreSet = new HashSet<String>(Arrays.asList(ignore));
        if(schema != null ) {
            final ObjectClassInfo oci = schema.findObjectClassInfo(ObjectClass.ACCOUNT_NAME);
            final Set<AttributeInfo> ais = oci.getAttributeInfo();
            for (AttributeInfo ai : ais) {
                //ignore not returned by default
                if (!ai.isReturnedByDefault()) {
                    ignoreSet.add(ai.getName());
                }
                //ignore not readable attributes
                if (!ai.isReadable()) {
                    ignoreSet.add(ai.getName());
                }
            }
        }
        
        Set<String> names = CollectionUtil.newCaseInsensitiveSet();
        names.addAll(expMap.keySet());
        names.addAll(actMap.keySet());
        names.removeAll(ignoreSet);
        names.remove(Uid.NAME);
        int missing = 0; 
        List<String> mis = new ArrayList<String>();
        List<String> extra = new ArrayList<String>();        
        for (String attrName : names) {
            final Attribute expAttr = expMap.get(attrName);
            final Attribute actAttr = actMap.get(attrName);
            if(expAttr != null && actAttr != null ) {
                assertEquals(attrName, expAttr, actAttr);
            } else {
                missing = missing + 1;
                if(expAttr != null) {
                    mis.add(expAttr.getName());
                }
                if(actAttr != null) {
                    extra.add(actAttr.getName());                    
                }
            }
        }
        assertEquals("missing attriburtes extra "+extra+" , missing "+mis, 0, missing); 
        log.ok("attributeSets are equal!");
    }       
    
    /**
     * @param cfg
     * @return
     */
    protected OracleERPConnector getConnector(OracleERPConfiguration cfg) {
        OracleERPConnector con = new OracleERPConnector();
        con.init(cfg);
        return con;
    }      
}
