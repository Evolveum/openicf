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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.GroupAttribute;
import org.junit.After;
import org.junit.Before;

public abstract class SolarisTestBase {
    /** this password is used to initialize all the test users */
    public static final String SAMPLE_PASSWD = "samplePasswd";
    private static final String testgroupName = "testgrp";
    private SolarisConnection connection;
    private SolarisConfiguration configuration;
    private ConnectorFacade facade;
    private boolean isTrustedExtensions;
    
    public boolean isTrustedExtensions() {
        return isTrustedExtensions;
    }

    public SolarisTestBase() {
        try {
            isTrustedExtensions = SolarisTestCommon.getProperty("isSolarisTx", Boolean.class);
        } catch (Exception ex) {
            // OK
        }
    }

    @Before
    public void beforeTestMethods() {
        connection = SolarisTestCommon.getSolarisConn();
        configuration = connection.getConfiguration();
        facade = SolarisTestCommon.createConnectorFacade(getConfiguration());

        System.out.println("TEST HOST: " + connection.getConfiguration().getHost());

        generateUsers();
        generateGroup(CollectionUtil.newList("root"));
    }

    @After
    public void afterTestMethods() {
        cleanUpUsers();
        cleanupGroup();
        try {
            if (connection != null) 
                connection.dispose();
        } catch (Exception ex) {
            //OK
        }
    }

    public SolarisConnection getConnection() {
        return connection;
    }

    public SolarisConfiguration getConfiguration() {
        return configuration;
    }

    public ConnectorFacade getFacade() {
        return facade;
    }

    private void generateUsers() {
        for (int i = 0; i < getCreateUsersNumber(); i++) {
            Set<Attribute> attrs = new HashSet<Attribute>();
            attrs.add(AttributeBuilder.build(Name.NAME, formatName(i)));
            attrs.add(AttributeBuilder.buildPassword(SAMPLE_PASSWD.toCharArray()));

            facade.create(ObjectClass.ACCOUNT, attrs, null);
        }
    }

    private void cleanUpUsers() {
        for (int i = 0; i < getCreateUsersNumber(); i++) {
            try {
                facade.delete(ObjectClass.ACCOUNT, new Uid(formatName(i)), null);
            } catch (RuntimeException ex) {
                // OK
            }
        }
    }
    
    /**
     * Test use a special format for usernames. Get the username created by 
     * the {@link SolarisTestBase} class, and control that the given {@code i}
     * is in given range from 0 to {@link SolarisTestBase#getCreateUsersNumber()}.
     * 
     * @param i the ID for the user.
     * @return the username for given iterator
     * @throws {@link RuntimeException} if the user with the given {@code i} wasn't created
     */
    public String getUsername(int i) {
        if (i >= getCreateUsersNumber() || i < 0)
            throw new RuntimeException("param 'i' is out of bounds.");
        
        return formatName(i);
    }

    static String formatName(int i) {
        return "test" + i;
    }

    private void generateGroup(List<String> usernames) {
        if (createGroup()) {
            Set<Attribute> attrs = new HashSet<Attribute>();
            attrs.add(AttributeBuilder.build(Name.NAME, testgroupName));
            attrs.add(AttributeBuilder.build(GroupAttribute.USERS.getName(), usernames));
            
            facade.create(ObjectClass.GROUP, attrs, null);
        }
    }

    private void cleanupGroup() {
        if (createGroup()) {
            try {
                facade.delete(ObjectClass.GROUP, new Uid(testgroupName), null);
            } catch (RuntimeException ex) {
                // OK
            }
        }
    }
    
    String getGroupName() {
        if (!createGroup()) {
            throw new RuntimeException("Group was not initialized. Change the Unit test's createGroup() value to true.");
        }
        return testgroupName;  
    }

    public abstract int getCreateUsersNumber();

    public abstract boolean createGroup();
}
