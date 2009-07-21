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
import static org.identityconnectors.oracleerp.OracleERPUtil.*;
import static org.junit.Assert.*;

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
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.test.common.TestHelpers;
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

    static final String CONFIG_SYSADM = "configuration.sysadm";
    static final String CONFIG_TST = "configuration.tst";
    static final String ACCOUNT_ALL_ATTRS = "account.all";
    static final String ACCOUNT_REQUIRED_ATTRS = "account.required";
    static final String ACCOUNT_MODIFY_ATTRS = "account.modify";
    static final String ACCOUNT_USER_ATTRS = "account.required";
    static final String ACCOUNT_OPTIONS = "account.options";
    static final String ACCOUNT_AUDITOR = "account.auditor";

    /**
     * Load configurations and attibuteSets Data provides 
     */
    static  DataProvider dataProvider = null; 

    
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
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testValidateSysConfiguration() {
        final OracleERPConfiguration config = getConfiguration(CONFIG_TST);
        assertNotNull(config);
        config.validate();
    }    

    /**
     * Test method for {@link OracleERPConnector#schema()}.
     */
    @Test
    public void testSchema() {
        Schema schema = getFacade(getConfiguration(CONFIG_SYSADM)).schema();
        // Schema should not be null
        assertNotNull(schema);
        Set<ObjectClassInfo> objectInfos = schema.getObjectClassInfo();
        assertNotNull(objectInfos);
        assertEquals(2, objectInfos.size());
    }


    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConfig() {
        final OracleERPConfiguration config = getConfiguration(CONFIG_SYSADM);
        assertNotNull(config);
        
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
        assertTrue(config.isReturnSobOrgAttrs());
        assertNotNull(config.getUserActions());        
        assertNotNull(config.getConnectionUrl());
    }
    
    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConnectorTest() {
        final OracleERPConfiguration config = getConfiguration(CONFIG_SYSADM);
        assertNotNull(config);
        config.validate();
        ConnectorFacade facade = getFacade(config);
        assertNotNull(facade);
        facade.test();
        OracleERPConnector conn = getConnector(CONFIG_SYSADM);
        conn.test();
    }

    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test
    public void testCreateRequiredOnly() {
        final OracleERPConnector c = getConnector(CONFIG_SYSADM); 
       
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
        
        final OperationOptionsBuilder oob = new OperationOptionsBuilder();
        addAllAttributesToGet(oob, c.getAccount().getObjectClassInfo().getAttributeInfo());
       
        List<ConnectorObject> results = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid), oob.build());
        assertTrue("expect 1 connector object", results.size() == 1);
        final ConnectorObject co = results.get(0);
        final Set<Attribute> returned = co.getAttributes();
        System.out.println(returned);
        
        testAttrSet(attrs, returned, OperationalAttributes.PASSWORD_NAME, OWNER);
    }
    
    
    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test
    public void testCreate() {
        final OracleERPConnector c = getConnector(CONFIG_SYSADM);
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_ALL_ATTRS);
        
        
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);

        List<ConnectorObject> results = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        assertTrue("expect 1 connector object", results.size() == 1);
        final ConnectorObject co = results.get(0);
        final Set<Attribute> returned = co.getAttributes();
        System.out.println(returned);
        
        // Date text representations are not the same, skiped due to extra test
        testAttrSet(attrs, returned, OperationalAttributes.PASSWORD_NAME,
                OperationalAttributes.PASSWORD_EXPIRED_NAME, OWNER, END_DATE, LAST_LOGON_DATE, PWD_DATE, START_DATE);
    }    
    
    
    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test
    public void testQueryAuditorData() {
        final OracleERPConnector c = getConnector(CONFIG_SYSADM);
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_ALL_ATTRS);
        final Set<Attribute> attrsOpt = getAttributeSet(ACCOUNT_OPTIONS);
        final Set<Attribute> expectedAttr = getAttributeSet(ACCOUNT_AUDITOR);
        
        final OperationOptionsBuilder oob = new OperationOptionsBuilder();
        addAuditorDataOptions(oob, attrsOpt);
        addAllAttributesToGet(oob, c.getAccount().getObjectClassInfo().getAttributeInfo());
        
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);

        List<ConnectorObject> results = TestHelpers.searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid), oob.build());
        assertTrue("expect 1 connector object", results.size() == 1);
        final ConnectorObject co = results.get(0);
        final Set<Attribute> returned = co.getAttributes();
        System.out.println(returned);
        
        testAttrSet(expectedAttr, returned);
    }    

    /**
     * 
     * @param oob
     * @param attrsOpt
     */
    private void addAuditorDataOptions(OperationOptionsBuilder oob, Set<Attribute> attrsOpt) {
        for (Attribute attr : attrsOpt) {
            oob.setOption(attr.getName(), AttributeUtil.getSingleValue(attr));
        }
    }

    /**
     * 
     * @param oob
     * @param attrsOpt
     */
    private void addAllAttributesToGet(OperationOptionsBuilder oob, Set<AttributeInfo> attrInfos) {
        Set<String> attrNames = CollectionUtil.newCaseInsensitiveSet();
        for (AttributeInfo ai : attrInfos) {
            if(ai.isReadable()) {
                attrNames.add(ai.getName());
            }
        }
        oob.setAttributesToGet(attrNames);
    }

    /**
     * 
     * @param oob
     * @param attrsOpt
     */
    private void addDefaultAttributesToGet(OperationOptionsBuilder oob, Set<AttributeInfo> attrInfos) {
        Set<String> attrNames = CollectionUtil.newCaseInsensitiveSet();
        for (AttributeInfo ai : attrInfos) {
            if(ai.isReadable() && ai.isReturnedByDefault()) {
                attrNames.add(ai.getName());
            }
        }
        oob.setAttributesToGet(attrNames);
    }    
    
    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test
    public void testUpdate() {
        final ConnectorFacade facade = getFacade(CONFIG_SYSADM);
        
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        final Uid uid = facade.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
        //assertEquals(tstName, uid.getUidValue());
        
    }    
    

    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test
    public void testDelete() {
        final ConnectorFacade facade = getFacade(CONFIG_SYSADM);
        
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        final Uid uid = facade.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
    }    
 

    /**
     * @param userName
     * @param testPassword2
     * @return
     */
    private Uid createUser() {
        final ConnectorFacade facade = getFacade(CONFIG_SYSADM);
        
        Set<Attribute> tuas = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        assertNotNull(tuas);
        return facade.create(ObjectClass.ACCOUNT, tuas, null);
    }     


    /**
     * @param userName
     */
    protected void quitellyDeleteUser(String name) {
        quitellyDeleteUser(new Uid(name));
    }     
    
    /**
     * @param userName
     */
    protected void quitellyDeleteUser(Uid uid) {
        final ConnectorFacade facade = getFacade(CONFIG_SYSADM);

        try{
            facade.delete(ObjectClass.ACCOUNT, uid, null);
        } catch (Exception ex) {
            log.error(ex, "expected");
            // handle exception
        }         
    }      
    
    
    /**
     * 
     * @param expected
     * @param actual
     * @param ignore
     */
    protected void testAttrSet(Set<Attribute> expected, Set<Attribute> actual, String ... ignore) {
        testAttrSet(AttributeUtil.toMap(expected), AttributeUtil.toMap(actual), false, new HashSet<String>(Arrays.asList(ignore)));              
    }    
    
       
    /**
     * 
     * @param expected
     * @param actual
     * @param fullMatch
     * @param ignore
     */
    protected void testAttrSet(Set<Attribute> expected, Set<Attribute> actual, boolean fullMatch, String ... ignore) {
        testAttrSet(AttributeUtil.toMap(expected), AttributeUtil.toMap(actual), fullMatch, new HashSet<String>(Arrays.asList(ignore)));              
    }      
    
    /**
     * S
     * @param expMap
     * @param currMap
     * @param fullMatch
     * @param ignoreSet
     */
    protected void testAttrSet(final Map<String, Attribute> expMap, final Map<String, Attribute> currMap, boolean fullMatch, Set<String> ignoreSet) {
        log.ok("attributeSetsEquals");
        Set<String> names = CollectionUtil.newCaseInsensitiveSet();
        names.addAll(expMap.keySet());
        if(fullMatch) {
            names.addAll(currMap.keySet());
        }
        names.removeAll(ignoreSet);
        names.remove(Uid.NAME);
        List<String> mis = new ArrayList<String>();
        List<String> ext = new ArrayList<String>();        
        for (String attrName : names) {
            final Attribute expAttr = expMap.get(attrName);
            final Attribute currAttr = currMap.get(attrName);
            if(expAttr != null && currAttr != null ) {                
                assertEquals(attrName, expAttr.getValue().toString(), currAttr.getValue().toString());
            } else {
                if(currAttr == null) {
                    mis.add(expAttr.getName());
                }
                if(expAttr == null) {
                    ext.add(currAttr.getName());                    
                }
            }
        }
        assertEquals("missing attriburtes "+mis, 0, mis.size()); 
        assertEquals("extra attriburtes "+ext, 0, ext.size()); 
        log.ok("expected attributes are equal to current");
    }       
    
    /**
     * 
     * @param setName
     * @return
     */
    protected  Set<Attribute> getAttributeSet(String setName) {
        Set<Attribute> ret = CollectionUtil.newSet(dataProvider.getAttributeSet(setName));
        Name attr = AttributeUtil.getNameFromAttributes(ret);
        if (attr != null) {
            final String value = AttributeUtil.getStringValue(attr) + System.currentTimeMillis();
            Attribute add = AttributeBuilder.build(Name.NAME, value );
            ret.remove(attr);
            ret.add(add);
        }
        return ret;
    }
    
    /**
     * 
     * @param configName
     * @return
     */
    protected ConnectorFacade getFacade(String configName) {
        OracleERPConfiguration config = getConfiguration(configName);
        assertNotNull(config);
        return getFacade(config); 
    } 
    
    /**
     * 
     * @param config
     * @return
     */
    protected ConnectorFacade getFacade(OracleERPConfiguration config) {
        final ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        assertNotNull(factory);
        final APIConfiguration impl = TestHelpers.createTestConfiguration(OracleERPConnector.class, config);
        assertNotNull(impl);
        final ConnectorFacade facade = factory.newInstance(impl);
        assertNotNull(facade);
        return facade;
    }    
    
    /**
     * 
     * @param config
     * @return
     */
    protected OracleERPConnector getConnector(OracleERPConfiguration config) {
        assertNotNull(config);
        OracleERPConnector con = new OracleERPConnector();
        assertNotNull(con);
        con.init(config);
        return con;
    }      

    
    /**
     * 
     * @param configName
     * @return
     */
    protected OracleERPConnector getConnector(String configName) {
        OracleERPConfiguration config = getConfiguration(configName);
        assertNotNull(config);
        return getConnector(config);
    }     
    
    /**
     * 
     * @param configName
     * @return
     */
    protected OracleERPConfiguration getConfiguration(String configName) {
        OracleERPConfiguration config = new OracleERPConfiguration();
        try {
            dataProvider.loadConfiguration(configName, config);
        } catch (Exception e) {            
            fail("load configuration "+configName+" error:"+ e.getMessage());
        }
        assertNotNull(config);
        return config;
    }    
    
    /**
     * Helper function to compare lists
     * @param vals
     * @return
     */
    private String attrToSortedStr(String vals) {
        final String delim = ",";
        StringBuilder bld = new StringBuilder();
        if (vals != null)  {
          String [] valArray = vals.split(delim);
          Arrays.sort(valArray);          
          for (String i: valArray) {
              if ( bld.length()!=0 ) {
                  bld.append(i);
              }
              bld.append(delim);              
          }
        }
        return bld.toString();
    } // attrToSortStr()  
}
