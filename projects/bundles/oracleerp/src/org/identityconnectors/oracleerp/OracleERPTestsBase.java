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

import static org.junit.Assert.*;
import static org.identityconnectors.oracleerp.OracleERPUtil.*;


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
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.Uid;
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
    
    protected static final String CONFIG_SYSADM = "configuration.sysadm";
    protected static final String CONFIG_TST = "configuration.tst";
    protected static final String CONFIG_USER = "configuration.user";
    /**
     * Load configurations and attibuteSets Data provides 
     */
    static DataProvider dataProvider = null;

    /**
     * The class load method
     */
    @BeforeClass
    public static void setUpClass() { 
        dataProvider = ConnectorHelper.createDataProvider();
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
     * @param attrsOpt
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
     * 
     * @param oob
     * @param attrsOpt
     */
    protected void addAuditorDataOptions(OperationOptionsBuilder oob, Set<Attribute> attrsOpt) {
        for (Attribute attr : attrsOpt) {
            oob.setOption(attr.getName(), AttributeUtil.getSingleValue(attr));
        }
    }

    /**
     * 
     * @param oob
     * @param attrsOpt
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
     * Helper function to compare lists
     * @param vals
     * @return
     */
    protected String attrToSortedStr(String vals) {
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

    /**
     * 
     * @param ret
     */
    protected String generateNameAttribute(Set<Attribute> ret) {
        Name attr = AttributeUtil.getNameFromAttributes(ret);
        String value = null;
        if (attr != null) {
            ret.remove(attr);
            value = AttributeUtil.getStringValue(attr) + System.currentTimeMillis();
            Attribute add = AttributeBuilder.build(Name.NAME, value );
            ret.add(add);
        }
        return value;
    }

    /**
     * 
     * @param ret
     * @param value
     */
    protected void replaceNameAttribute(Set<Attribute> ret, String value) {
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
     * @return
     */
    protected Set<Attribute> getAttributeSet(String setName) {
        Set<Attribute> ret = CollectionUtil.newSet(dataProvider.getAttributeSet(setName));
        return ret;
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
     * @param configName
     * @return
     */
    protected ConnectorFacade getFacade(String configName) {
        OracleERPConfiguration config = getConfiguration(configName);
        assertNotNull(config);
        return getFacade(config); 
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
                testAttribute(attrName, expAttr, currAttr);
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
            String exp = expVals.get(i).toString();
            String curr = currVals.get(i).toString();
            if(attrName.contains("date")) {
                int min = Math.min(exp.length(), curr.length());
                assertEquals(attrName+":["+i+"]", exp.substring(0, min), curr.substring(0, min));
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

    /**
     * @param userName
     * @param testPassword2
     * @return
     */
    protected Uid createUser() {
        final ConnectorFacade facade = getFacade(CONFIG_SYSADM);
        
        Set<Attribute> tuas = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        generateNameAttribute(tuas);
        assertNotNull(tuas);
        return facade.create(ObjectClass.ACCOUNT, tuas, null);
    }         

}