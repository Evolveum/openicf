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
package org.identityconnectors.solaris.test;

import static org.identityconnectors.solaris.test.SolarisTestCommon.getTestProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OpDeleteImplTest {
    
    private SolarisConfiguration config;
    private ConnectorFacade facade;
    /** only for verification of results, the rest of test methods should be called on the facade. */
    private SolarisConnector testConnector;

    /**
     * set valid credentials based on build.groovy property file
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        config = SolarisTestCommon.createConfiguration();
        facade = SolarisTestCommon.createConnectorFacade(config);
        testConnector = SolarisTestCommon.createConnector(config);
        
        SolarisTestCommon.printIPAddress(config);
    }

    @After
    public void tearDown() throws Exception {
        config = null;
        facade = null;
        testConnector = null;
    }
    
    @Test (expected=ConnectorException.class)
    public void testDeleteUnknownUid() {
        facade.delete(ObjectClass.ACCOUNT, new Uid("NONEXISTING_UID____"), null);
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void unknownObjectClass() {
        final Set<Attribute> attrs = initSampleUser();
        facade.delete(new ObjectClass("NONEXISTING_OBJECTCLASS"), getUid(attrs), null);
    }
    
    @Test (expected=ConnectorException.class)
    public void testDeleteUnknownGroup() {
    	facade.delete(ObjectClass.GROUP, new Uid("nonExistingGroup"), null);
    }
    
    @Test
    public void testDeleteGroup() {
    	
    	Set<Attribute> attrs = SolarisTestCommon.initSampleGroup("sampleGroup", "root");
    	Map<String, Attribute> grpAttrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
    	String groupName = ((Name) grpAttrMap.get(Name.NAME)).getNameValue();
    	// create the group
    	facade.create(ObjectClass.GROUP, attrs, null);
    	try {
    		// verify if it exists
    		String out = checkIfGroupExists(groupName);
    		Assert.assertTrue(out.contains(groupName));
    		//perform delete
    		facade.delete(ObjectClass.GROUP, new Uid(groupName), null);
    		try {
    			checkIfGroupExists(groupName);
    			Assert.fail("delete failed, the group is still on the resource: '" + groupName + "'");
    		} catch (ConnectorException ex) {
    			// ok
    		}
    	} finally {
    		try {
    			testConnector.getConnection().executeCommand("cat /etc/group | grep '" + groupName + "'", Collections.<String>emptySet(), CollectionUtil.newSet(groupName));
    			Assert.fail("group '" + groupName + "' was not deleted.");
    		} catch (ConnectorException ex) {
    			// OK
    		}
    	}
    }

    /** 
     * @param groupName the group name to search
     * @return the output fetched from the group search command. If successful, it should contain the 'groupname' parameter.
     * @throws {@link ConnectorException} if the group was not found.
     */
	private String checkIfGroupExists(String groupName) {
		return testConnector.getConnection().executeCommand("cat /etc/group | grep '" + groupName + "'", Collections.<String>emptySet(), CollectionUtil.newSet(groupName));
	}
    

    /* ************* AUXILIARY METHODS *********** */

    /** fill in sample user/password for sample user used in create */
    private Set<Attribute> initSampleUser() {
        Set<Attribute> res = new HashSet<Attribute>();
        
        res.add(AttributeBuilder.build(Name.NAME, getTestProperty("sampleUser")));
        
        String samplePasswd = getTestProperty("samplePasswd");
        res.add(AttributeBuilder.buildPassword(new GuardedString(samplePasswd.toCharArray())));
        
        return res;
    }
    
    private Uid getUid(Set<Attribute> attrs) {
        // Read only list of attributes
        final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
        final String username = ((Name) attrMap.get(Name.NAME)).getNameValue();
        return new Uid(username);
    }

}
