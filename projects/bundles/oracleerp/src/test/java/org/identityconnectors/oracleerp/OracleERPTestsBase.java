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

import static org.identityconnectors.oracleerp.OracleERPUtil.DEFAULT_DRIVER;
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
import org.identityconnectors.contract.data.GroovyDataProvider;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.BeforeClass;


/**
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
abstract public class OracleERPTestsBase {

    private static final Log log = Log.getLog(OracleERPTestsBase.class);
    
    protected static final String ACCOUNT_ALL_ATTRS = "account.all";
    protected static final String ACCOUNT_AUDITOR = "account.auditor";
    protected static final String ACCOUNT_MODIFY_ATTRS = "account.modify";
    protected static final String ACCOUNT_OPTIONS = "account.options";
    protected static final String ACCOUNT_REQUIRED_ATTRS = "account.required";
    protected static final String ACCOUNT_USER_ATTRS = "account.required";
    protected static final String ACCOUNT_ENABLED = "account.enabled";
    protected static final String ACCOUNT_DISSABLED = "account.dissabled";
    
    protected static final String CONFIG_SYSADM = "configuration.sysadm";
    protected static final String CONFIG_TST = "configuration.tst";
    protected static final String CONFIG_USER = "configuration.user";
    
    protected static final String GROOVY = "GROOVY";
    protected static final String RUN_ERROR_ACTION = "org.identityconnectors.oracleerp.OracleERPConnector/config/RunErrorAction.groovy";

    /**
     * Load configurations and attibuteSets Data provides 
     */
    static DataProvider dataProvider = null;

    /**
     * The class load method
     */
    @BeforeClass
    public static void setUpClass() { 
        dataProvider = new GroovyDataProvider(OracleERPConnector.class.getName());
    }

    /**
     * 
     */
    public OracleERPTestsBase() {
        super();
    }

    /**
     * 
     * @param oob
     * @param attrInfos
     */
    protected void addAllAttributesToGet(OperationOptionsBuilder oob, Set<AttributeInfo> attrInfos) {
        Set<String> attrNames = CollectionUtil.newCaseInsensitiveSet();
        for (AttributeInfo ai : attrInfos) {
            if(ai.isReadable()) {
                attrNames.add(ai.getName());
            }
        }
        oob.setAttributesToGet(attrNames);
    }

    /**
     * Add one option converted from the attribute set
     * @param oob option builder {@link OperationOptionsBuilder}
     * @param attrsOpt the attribute {@link Attribute} set
     */
    protected void addAuditorDataOptions(OperationOptionsBuilder oob, Set<Attribute> attrsOpt) {
        for (Attribute attr : attrsOpt) {
            oob.setOption(attr.getName(), AttributeUtil.getSingleValue(attr));
        }
    }

    /**
     * Add default attribute to get to operation option builder from the attribute info set
     * It select all attributes with are {@link Flags} readable and returned by default
     * @param oob oob option builder {@link OperationOptionsBuilder}
     * @param attrInfos The attribute {@link AttributeInfo} info set from the schema {@link Schema}
     */
    protected void addDefaultAttributesToGet(OperationOptionsBuilder oob, Set<AttributeInfo> attrInfos) {
        Set<String> attrNames = CollectionUtil.newCaseInsensitiveSet();
        for (AttributeInfo ai : attrInfos) {
            if(ai.isReadable() && ai.isReturnedByDefault()) {
                attrNames.add(ai.getName());
            }
        }
        oob.setAttributesToGet(attrNames);
    }  

    /**
     * Replace name attribute in the attribute set by generated value
     * @param attrs a set of attributes {@link Attribute}
     * @return replaced String name
     */
    protected String replaceNameByRandom(Set<Attribute> attrs) {
        Name attr = AttributeUtil.getNameFromAttributes(attrs);
        String value = null;
        if (attr != null) {
            attrs.remove(attr);
            value = AttributeUtil.getStringValue(attr) + System.currentTimeMillis();
            Attribute add = AttributeBuilder.build(Name.NAME, value );
            attrs.add(add);
        } 
        return value;
    }

    /**
     * 
     * @param ret
     * @param value
     */
    protected void replaceNameByValue(Set<Attribute> ret, String value) {
        Name attr = AttributeUtil.getNameFromAttributes(ret);
        if (attr != null) {
            ret.remove(attr);
        }
        Attribute add = AttributeBuilder.build(Name.NAME, value );
        ret.add(add);
    }

    /**
     * 
     * @param setName
     * @return attribute Set
     */
    protected Set<Attribute> getAttributeSet(String setName) {
        Set<Attribute> ret = CollectionUtil.newSet(dataProvider.getAttributeSet(setName));
        return ret;
    }

    /**
     * 
     * @param configName
     * @return OracleERPConfiguration
     */
    protected OracleERPConfiguration getConfiguration(String configName) {
        OracleERPConfiguration config = new OracleERPConfiguration();
        try {
            dataProvider.loadConfiguration(configName, config);
        } catch (Exception e) {            
            fail("load configuration "+configName+" error:"+ e.getMessage());
        }
        assertNotNull(config);
        assertEquals("The driver is not defined, the dataprovider is not initialized. Set up:" +
        		     "-Dproject.name=connector-oracleerp "+
                     "-Ddata-provider=org.identityconnectors.contract.data.GroovyDataProvider"+
                     "-DbundleJar=dist/org.identityconnectors.oracleerp-1.0.1.jar"+
                     "-DbundleName=org.identityconnectors.oracleerp"+
                     "-DbundleVersion=1.0.1 ", DEFAULT_DRIVER, config.getDriver());
        config.setConnectorMessages(TestHelpers.createDummyMessages());
        return config;
    }

    /**
     * 
     * @param config
     * @return OracleERPConnector
     */
    protected OracleERPConnector getConnector(OracleERPConfiguration config) {
        assertNotNull(config);
        OracleERPConnector c = new OracleERPConnector();
        assertNotNull(c);
        c.init(config);
        return c;
    }

    /**
     * 
     * @param configName
     * @return OracleERPConnector
     */
    protected OracleERPConnector getConnector(String configName) {
        OracleERPConfiguration config = getConfiguration(configName);
        assertNotNull(config);
        return getConnector(config);
    }

    /**
     * 
     * @param config
     * @return ConnectorFacade
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
     * @param configName
     * @return ConnectorFacade
     */
    protected ConnectorFacade getFacade(String configName) {
        OracleERPConfiguration config = getConfiguration(configName);
        assertNotNull(config);
        return getFacade(config); 
    }

    /**
     * Test two attribute sets
     * 
     * @param expMap expected
     * @param currMap current
     * @param fullMatch true for both side match, false for test those in expected are exist and equal in actual
     * @param ignoreSet {@link Set} attribute names being ignores
     */
    protected void testAttrSet(final Map<String, Attribute> expMap,
            final Map<String, Attribute> currMap, boolean fullMatch,
            Set<String> ignoreSet) {
        log.info("attributeSetsEquals");
        Set<String> names = CollectionUtil.newCaseInsensitiveSet();
        names.addAll(expMap.keySet());
        if (fullMatch) {
            names.addAll(currMap.keySet());
        }
        names.removeAll(ignoreSet);
        names.remove(Uid.NAME);
        List<String> mis = new ArrayList<String>();
        List<String> ext = new ArrayList<String>();
        for (String attrName : names) {
            final Attribute expAttr = expMap.get(attrName);
            final Attribute currAttr = currMap.get(attrName);
            if (expAttr != null && currAttr != null) {
                testAttribute(attrName, expAttr, currAttr);
            } else {
                if (expAttr == null && currAttr != null) {
                    ext.add(currAttr.getName());
                }
                if (currAttr == null && expAttr != null) {
                    mis.add(expAttr.getName());
                }
            }
        }
        assertEquals("missing attriburtes " + mis, 0, mis.size());
        assertEquals("extra attriburtes " + ext, 0, ext.size());
        log.info("expected attributes are equal to current");
    }

    /**
     * @param attrName
     * @param expAttr
     * @param currAttr
     */
    private void testAttribute(String attrName, final Attribute expAttr, final Attribute currAttr) {
        final List<Object> expVals = expAttr.getValue();
        final List<Object> currVals = currAttr.getValue();
        assertEquals("Size of attribute:"+attrName, expVals.size(), currVals.size());
    
        for (int i = 0; i < expVals.size(); i++) {
            if( expVals.get(i) == currVals.get(i)) {
                continue;
            }            
            String exp = expVals.get(i) == null ? "null" : expVals.get(i).toString();
            String curr = currVals.get(i) == null ? "null" : currVals.get(i).toString();
            if(attrName.contains("date")) {
                assertEquals(attrName+":["+i+"]", OracleERPUtil.normalizeStrDate(exp) , OracleERPUtil.normalizeStrDate(curr));
            } else {
                assertEquals(attrName+":["+i+"]", exp, curr);
            }
            
        }
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
     * 
     * @param expected
     * @param actual
     * @param ignore
     */
    protected void testAttrSet(Set<Attribute> expected, Set<Attribute> actual, String ... ignore) {
        testAttrSet(AttributeUtil.toMap(expected), AttributeUtil.toMap(actual), false, new HashSet<String>(Arrays.asList(ignore)));              
    }
}
