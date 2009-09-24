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
package org.identityconnectors.racf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;

import junit.framework.Assert;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public abstract class RacfConnectorTestBase {
    // Connector Configuration information
    //
    protected static String       HOST_NAME;
    protected static String       SYSTEM_PASSWORD;
    protected static String       SYSTEM_USER;
    protected static String       SYSTEM_USER_LDAP;
    protected static String       SUFFIX;
    protected static String       TEST_USER = "TEST106";
    protected static String       TEST_USER2 = "TEST106A";
    protected static String       TEST_GROUP1 = "IDMGRP01";
    protected static String       TEST_GROUP2 = "IDMGRP02";
    protected static String       TEST_GROUP3 = "IDMGRP03";
    protected Uid                 TEST_USER_UID;
    protected Uid                 TEST_USER_UID2;
    protected Uid                 TEST_GROUP1_UID;
    protected Uid                 TEST_GROUP2_UID;
    protected Uid                 TEST_GROUP3_UID;

    protected static final int     HOST_LDAP_PORT   = 389;
    protected static final int     HOST_TELNET_PORT = 23;
    protected static final Boolean USE_SSL          = Boolean.FALSE;
    
    protected static final String GROUP_RACF_PARSER   = "org/identityconnectors/racf/GroupRacfSegmentParser.xml";
    protected static final String RACF_PARSER         = "org/identityconnectors/racf/RacfSegmentParser.xml";
    protected static final String CICS_PARSER         = "org/identityconnectors/racf/CicsSegmentParser.xml";
    protected static final String OMVS_PARSER         = "org/identityconnectors/racf/OmvsSegmentParser.xml";
    protected static final String TSO_PARSER          = "org/identityconnectors/racf/TsoSegmentParser.xml";
    protected static final String NETVIEW_PARSER      = "org/identityconnectors/racf/NetviewSegmentParser.xml";
    protected static final String CATALOG_PARSER      = "org/identityconnectors/racf/CatalogParser.xml";

    @Before
    public void before() {
        TEST_USER_UID     = makeUid(TEST_USER, ObjectClass.ACCOUNT);
        TEST_USER_UID2    = makeUid(TEST_USER2, ObjectClass.ACCOUNT);
        TEST_GROUP1_UID   = makeUid(TEST_GROUP1, RacfConnector.RACF_GROUP);
        TEST_GROUP2_UID   = makeUid(TEST_GROUP2, RacfConnector.RACF_GROUP);
        TEST_GROUP3_UID   = makeUid(TEST_GROUP3, RacfConnector.RACF_GROUP);
        System.out.println("------------ New Test ---------------");
    }

    @Test//@Ignore
    public void testListAllUsers() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            TestHandler handler = new TestHandler();
            TestHelpers.search(connector,ObjectClass.ACCOUNT, null, handler, null);
            int count = 0;
            for (ConnectorObject user : handler) {
                count++;
                System.out.println("Read User:"+user.getUid().getValue());
            }
            System.out.println("saw "+count);
        } finally {
            connector.dispose();
        }
    }

    @Test//@Ignore
    public void testListAllGroups() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            TestHandler handler = new TestHandler();
            TestHelpers.search(connector, RacfConnector.RACF_GROUP, null, handler, null);
            int count = 0;
            for (ConnectorObject group : handler) {
                count++;
                System.out.println("Read Group:"+group.getUid().getValue());
            }
            System.out.println("saw "+count);
        } finally {
            connector.dispose();
        }
    }
/*
    @Test//@Ignore
    public void testXXX() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            String output = connector._clUtil.getCommandOutput("HELP ALTUSER");
            System.out.println(output);
        } finally {
            connector.dispose();
        }
    }
*/
    @Test//@Ignore
    public void testListAllUsersNameOnly() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            TestHandler handler = new TestHandler();
            Map<String, Object> map = new HashMap<String, Object>();
            String[] attributesToGet = { Name.NAME };
            map.put(OperationOptions.OP_ATTRIBUTES_TO_GET, attributesToGet);
            OperationOptions options = new OperationOptions(map);
            TestHelpers.search(connector,ObjectClass.ACCOUNT, null, handler, options);
            int count = 0;
            for (ConnectorObject user : handler) {
                count++;
                System.out.println("Read User:"+user.getUid().getValue());
            }
            System.out.println("saw "+count);
        } finally {
            connector.dispose();
        }
    }

    @Test//@Ignore
    public void testListAllGroupsNameOnly() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            TestHandler handler = new TestHandler();
            Map<String, Object> map = new HashMap<String, Object>();
            String[] attributesToGet = { Name.NAME };
            map.put(OperationOptions.OP_ATTRIBUTES_TO_GET, attributesToGet);
            OperationOptions options = new OperationOptions(map);
            TestHelpers.search(connector, RacfConnector.RACF_GROUP, null, handler, options);
            int count = 0;
            for (ConnectorObject group : handler) {
                count++;
                System.out.println("Read Group:"+group.getUid().getValue());
            }
            System.out.println("saw "+count);
        } finally {
            connector.dispose();
        }
    }

    @Test//@Ignore
    public void testGetSpecifiedUser() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            Set<Attribute> attrs = fillInSampleUser(TEST_USER2);
            
            // Create the account if it doesn't already exist
            //
            deleteUser(TEST_USER_UID2, connector);
            connector.create(ObjectClass.ACCOUNT, attrs, null);
    
            boolean found = false;
            Map<String, Object> optionsMap = new HashMap<String, Object>();
            optionsMap.put(OperationOptions.OP_ATTRIBUTES_TO_GET, new String[] {Name.NAME});
            OperationOptions options = new OperationOptions(optionsMap);
            {
                int count = 0;
                TestHandler handler = new TestHandler();
                TestHelpers.search(connector,ObjectClass.ACCOUNT, new EqualsFilter(TEST_USER_UID2), handler, options);
                for (ConnectorObject user : handler) {
                    System.out.println(user);
                    if (equals(TEST_USER_UID2, user.getUid()))
                        found = true;
                    count++;
                }
                Assert.assertTrue(found);
                Assert.assertTrue(count==1);
            }
            {
                int count = 0;
                TestHandler handler = new TestHandler();
                TestHelpers.search(connector,ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(Name.NAME, makeUid(TEST_USER2, ObjectClass.ACCOUNT).getUidValue())), handler, options);
                for (ConnectorObject user : handler) {
                    System.out.println(user);
                    if (TEST_USER_UID2.equals(user.getUid()))
                        found = true;
                    count++;
                }
                Assert.assertTrue(found);
                Assert.assertTrue(count==1);
            }
            //TODO: these aren't real meaningful for ldap ids...
            {
                int count = 0;
                TestHandler handler = new TestHandler();
                TestHelpers.search(connector,ObjectClass.ACCOUNT, new StartsWithFilter(AttributeBuilder.build(Name.NAME, TEST_USER2)), handler, options);
                for (ConnectorObject user : handler) {
                    System.out.println(user);
                    if (TEST_USER_UID2.equals(user.getUid()))
                        found = true;
                    count++;
                }
                Assert.assertTrue(found);
            }
            {
                int count = 0;
                TestHandler handler = new TestHandler();
                TestHelpers.search(connector,ObjectClass.ACCOUNT, new EndsWithFilter(AttributeBuilder.build(Name.NAME, TEST_USER2)), handler, options);
                for (ConnectorObject user : handler) {
                    System.out.println(user);
                    if (TEST_USER_UID2.equals(user.getUid()))
                        found = true;
                    count++;
                }
                Assert.assertTrue(found);
            }
            {
                int count = 0;
                TestHandler handler = new TestHandler();
                TestHelpers.search(connector,ObjectClass.ACCOUNT, new ContainsFilter(AttributeBuilder.build(Name.NAME, TEST_USER2)), handler, options);
                for (ConnectorObject user : handler) {
                    System.out.println(user);
                    if (TEST_USER_UID2.equals(user.getUid()))
                        found = true;
                    count++;
                }
                Assert.assertTrue(found);
            }
        } finally {
            connector.dispose();
        }
    }
    
    @Test//@Ignore
    public void testGetSpecifiedGroup() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            boolean found = false;
            int count = 0;
            TestHandler handler = new TestHandler();
            Map<String, Object> optionsMap = new HashMap<String, Object>();
            optionsMap.put(OperationOptions.OP_ATTRIBUTES_TO_GET, new String[] {Name.NAME, getGroupMembersAttributeName(), getSupgroupAttributeName(), getOwnerAttributeName(), getInstallationDataAttributeName() });
            OperationOptions options = new OperationOptions(optionsMap);
            TestHelpers.search(connector,RacfConnector.RACF_GROUP, new EqualsFilter(AttributeBuilder.build(Name.NAME, "SYS1")), handler, options);
            for (ConnectorObject group : handler) {
                displayConnectorObject(group);
                if (equals(makeUid("SYS1", RacfConnector.RACF_GROUP), group.getUid()))
                    found = true;
                count++;
            }
            Assert.assertTrue(found);
            Assert.assertTrue(count==1);
        } finally {
            connector.dispose();
        }
    }

    boolean equals(Uid one, Uid two) {
        return one.getUidValue().equalsIgnoreCase(two.getUidValue());
    }
    @Test//@Ignore
    public void testModifyUser() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            displayConnectorObject(getUser(makeUid("IDM01", ObjectClass.ACCOUNT).getUidValue(), connector));
//            displayUser(getUser("CICSUSER", connector));
            // Delete the user
            deleteUser(TEST_USER_UID, connector);
    
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
            connector.create(ObjectClass.ACCOUNT, attrs, null);
            ConnectorObject user = getUser(makeUid(TEST_USER, ObjectClass.ACCOUNT).getUidValue(), connector);
            {
                Set<Attribute> changed = new HashSet<Attribute>();
                //
                changed.add(AttributeBuilder.build(getInstallationDataAttributeName(), "modified data"));
                List<Object> attributes = new LinkedList<Object>();
                attributes.add("SPECIAL");
                attributes.add("OPERATIONS");
                Attribute attributesAttr = AttributeBuilder.build(getAttributesAttributeName(), attributes);
                changed.add(attributesAttr);
                changed.add(user.getUid());
                connector.update(ObjectClass.ACCOUNT, changed, null);
                ConnectorObject object = getUser(makeUid(TEST_USER, ObjectClass.ACCOUNT).getUidValue(), connector);
                assertAttribute(attributesAttr, object);
            }
            {
                Set<Attribute> changed = new HashSet<Attribute>();
                //
                Attribute disableDate = AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME, new Date("11/12/2010").getTime());
                changed.add(disableDate);
                changed.add(user.getUid());
                connector.update(ObjectClass.ACCOUNT, changed, null);
                ConnectorObject object = getUser(makeUid(TEST_USER, ObjectClass.ACCOUNT).getUidValue(), connector);
                assertAttribute(disableDate, object);
            }
            {
                Set<Attribute> changed = new HashSet<Attribute>();
                //
                Attribute size = AttributeBuilder.build(getTsoSizeName(), Integer.valueOf(1000)); 
                changed.add(size);
                changed.add(user.getUid());
                connector.update(ObjectClass.ACCOUNT, changed, null);
                ConnectorObject object = getUser(makeUid(TEST_USER, ObjectClass.ACCOUNT).getUidValue(), connector, new String[] {getTsoSizeName()});
                assertAttribute(size, object);
            }
            {
                Set<Attribute> changed = new HashSet<Attribute>();
                //
                Attribute enableDate = AttributeBuilder.build(OperationalAttributes.ENABLE_DATE_NAME, new Date("11/15/2010").getTime());
                changed.add(AttributeBuilder.build(getInstallationDataAttributeName(), "modified data"));
                changed.add(enableDate);
                changed.add(user.getUid());
                connector.update(ObjectClass.ACCOUNT, changed, null);
                ConnectorObject object = getUser(makeUid(TEST_USER, ObjectClass.ACCOUNT).getUidValue(), connector);
                assertAttribute(enableDate, object);
            }
            {
                Set<Attribute> changed = new HashSet<Attribute>();
                //
                changed.add(AttributeBuilder.build(getInstallationDataAttributeName(), "modified data"));
                List<Object> attributes = new LinkedList<Object>();
                attributes.add("SPECIAL");
                attributes.add("OPERATOR");
                changed.add(AttributeBuilder.build(getAttributesAttributeName(), attributes));
                changed.add(user.getUid());
                try {
                    connector.update(ObjectClass.ACCOUNT, changed, null);
                    Assert.fail("Command should have failed");
                } catch (ConnectorException ce) {
                    System.out.println(ce);
                }
            }
    
            ConnectorObject changedUser = getUser(makeUid(TEST_USER, ObjectClass.ACCOUNT).getUidValue(), connector);
            //Attribute racfInstallationData = changedUser.getAttributeByName("racfinstallationdata");
            Attribute racfInstallationData = changedUser.getAttributeByName(getInstallationDataAttributeName());
            displayConnectorObject(changedUser);
            Assert.assertTrue(AttributeUtil.getStringValue(racfInstallationData).trim().equalsIgnoreCase("modified data"));
            displayConnectorObject(getUser(makeUid("IDM01", ObjectClass.ACCOUNT).getUidValue(), connector));
            displayConnectorObject(getUser(makeUid("IDM01", ObjectClass.ACCOUNT).getUidValue(), connector));
        } finally {
            connector.dispose();
        }
    }
    
    void assertAttribute(Attribute attribute, ConnectorObject object) {
        if (attribute.getName().equals(getAttributesAttributeName())) {
            Set<Object> set1 = new HashSet<Object>(attribute.getValue());
            Attribute attribute2 = object.getAttributeByName(attribute.getName());
            Assert.assertNotNull(attribute2);
            Set<Object> set2 = new HashSet<Object>(attribute.getValue());
            Assert.assertEquals(set1, set2);
            // must compare as sets
        } else {
            Assert.assertEquals(object.getAttributeByName(attribute.getName()), attribute);
        }
    }
    
    private String makeLine(String string, int length) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(string);
        while (buffer.length()<length)
            buffer.append(" ");
        return buffer.toString()+"\n";
    }
    
    protected String loadParserFromFile(String fileName) throws IOException {
        BufferedReader is = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(fileName)));
        try {
            StringBuffer tsoParser = new StringBuffer();
            String line = null;
            while ((line=is.readLine())!=null) {
                tsoParser.append(line+"\n");
            }
            return tsoParser.toString();
        } finally {
            is.close();
        }
    }

    protected void displayConnectorObject(ConnectorObject user) {
        Set<Attribute> attributes = user.getAttributes();
        for (Attribute attribute : attributes) {
            System.out.println(attribute.getName());
            List<Object> values = attribute.getValue();
            if (values==null) {
            	System.out.println("    <null value>");
            } else {
	            for (Object value : values) {
	                if (value==null)
	                    System.out.println("    <null>");
	                else
	                    System.out.println("    "+value.getClass().getName()+":"+value);
	            }
            }
        }
    }

    protected ConnectorObject getUser(String accountId, RacfConnector connector) throws Exception  {
        return getUser(accountId, connector, null);
    }

    protected ConnectorObject getUser(String accountId, RacfConnector connector, String[] attributes) throws Exception  {
        TestHandler handler = new TestHandler();
        OperationOptions options = null;
        if (attributes!=null) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(OperationOptions.OP_ATTRIBUTES_TO_GET, attributes);
            options = new OperationOptions(map);
        }
        //TestHelpers.search(connector,ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build("racfid", accountId.getUidValue())), handler, null);
        TestHelpers.search(connector,ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(Name.NAME, accountId)), handler, options);
        for (ConnectorObject user : handler) {
            if (accountId.equalsIgnoreCase(user.getName().getNameValue()))
                return user;
        }
        return null;
    }

    @Test//@Ignore
    public void testDumpSchema() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            Schema schema = connector.schema();
            System.out.print("Schema.oclasses = ");
            char separator = '[';
            for (ObjectClassInfo ocInfo : schema.getObjectClassInfo()) {
                System.out.print(separator+" \""+ocInfo.getType()+"\"");
                separator = ',';
            }
            System.out.println("]");
    
            for (ObjectClassInfo ocInfo : schema.getObjectClassInfo()) {
                System.out.print("Schema.attributes."+ocInfo.getType()+".oclasses = ");
                separator = '[';
                for (AttributeInfo aInfo : ocInfo.getAttributeInfo()) {
                    System.out.print(separator+" \""+aInfo.getName()+"\"");
                    separator = ',';
                }
                System.out.println("]");
            }
    
            for (ObjectClassInfo ocInfo : schema.getObjectClassInfo()) {
                for (AttributeInfo aInfo : ocInfo.getAttributeInfo()) {
                    System.out.println("Schema.\""+aInfo.getName()+"\".attribute."+ocInfo.getType()+".oclasses = [");
                    System.out.println("\ttype              : "+aInfo.getType().getName()+".class,");
                    System.out.println("\treadable          : "+aInfo.isReadable()+",");
                    System.out.println("\tcreateable        : "+aInfo.isCreateable()+",");
                    System.out.println("\tupdateable        : "+aInfo.isUpdateable()+",");
                    System.out.println("\trequired          : "+aInfo.isRequired()+",");
                    System.out.println("\tmultiValue        : "+aInfo.isMultiValued()+",");
                    System.out.println("\treturnedByDefault : "+aInfo.isReturnedByDefault());
                    System.out.println("]\n");
                }
            }
        } finally {
            connector.dispose();
        }
    }

    @Test//@Ignore
    public void testCreate() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
    
            // Delete the account if it already exists
            //
            deleteUser(TEST_USER_UID, connector);
    
            // Create the account
            //
            Uid newUid = connector.create(ObjectClass.ACCOUNT, attrs, null);
            System.out.println(newUid.getValue()+" created");
        } finally {
            connector.dispose();
        }
    }

    @Test//@Ignore
    public void testResolve() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
    
            // Delete the account if it already exists
            //
            deleteUser(TEST_USER_UID, connector);
            try {
                connector.resolveUsername(ObjectClass.ACCOUNT, TEST_USER, new OperationOptions(new HashMap()));
                Assert.fail("exception expected");
            } catch (UnknownUidException ue) {
                // expected
            }
    
            // Create the account
            //
            Uid newUid = connector.create(ObjectClass.ACCOUNT, attrs, null);
            System.out.println(newUid.getValue()+" created");
            Uid retrievedUid = connector.resolveUsername(ObjectClass.ACCOUNT, TEST_USER, new OperationOptions(new HashMap()));
            Assert.assertEquals(newUid, retrievedUid);
        } finally {
            connector.dispose();
        }
    }

    @Test
    @Ignore
    public void testChangePassword() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
    
            // Delete the account if it already exists
            //
            deleteUser(TEST_USER_UID, connector);
    
            // Create the account
            //
            Uid newUid = connector.create(ObjectClass.ACCOUNT, attrs, null);
            System.out.println(newUid.getValue()+" created");
    
            // Now, create a configuration for the user we created
            // and change password
            //
            RacfConfiguration userConfig = createUserConfiguration();
            RacfConnector userConnector = createConnector(userConfig);
            ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
            builder.setUid(TEST_USER_UID);
            Attribute password = AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, "xyzzy123");
            Attribute current_password = AttributeBuilder.build(OperationalAttributes.CURRENT_PASSWORD_NAME, "password");
            builder.addAttribute(current_password);
            builder.addAttribute(password);
            ConnectorObject newUser = builder.build();
            Set<Attribute> changeSet = CollectionUtil.newSet(newUser.getAttributes());
            changeSet.remove(newUser.getName());
            userConnector.update(ObjectClass.ACCOUNT, changeSet, null);
        } finally {
            connector.dispose();
        }
    }

    protected void deleteUser(final Uid testUser, RacfConnector connector) {
        try {
            connector.delete(ObjectClass.ACCOUNT, testUser, null);
        } catch (UnknownUidException rte) {
            // Ignore
        }
    }

    protected void deleteGroup(final Uid group, RacfConnector connector) {
        try {
            connector.delete(RacfConnector.RACF_GROUP, group, null);
        } catch (UnknownUidException rte) {
            // Ignore
        }
    }

    @Test//@Ignore
    public void testGroups() throws Exception {
        
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            deleteUser(TEST_USER_UID2, connector);
            deleteGroup(TEST_GROUP1_UID, connector);
            deleteGroup(TEST_GROUP2_UID, connector);
            deleteGroup(TEST_GROUP3_UID, connector);
            
            Map<String, Object> optionsMap = new HashMap<String, Object>();;
            OperationOptions options = new OperationOptions(optionsMap); 
            {
                Set<Attribute> groupAttrs = new HashSet<Attribute>();
                groupAttrs.add(new Name(TEST_GROUP1_UID.getUidValue()));
                Uid groupUid = connector.create(RacfConnector.RACF_GROUP, groupAttrs, options);
                Assert.assertNotNull(groupUid);
            }
            {
                Set<Attribute> groupAttrs = new HashSet<Attribute>();
                groupAttrs.add(new Name(TEST_GROUP2_UID.getUidValue()));
                Uid groupUid = connector.create(RacfConnector.RACF_GROUP, groupAttrs, options);
                Assert.assertNotNull(groupUid);
            }
            {
                Set<Attribute> groupAttrs = new HashSet<Attribute>();
                groupAttrs.add(new Name(TEST_GROUP3_UID.getUidValue()));
                Uid groupUid = connector.create(RacfConnector.RACF_GROUP, groupAttrs, options);
                Assert.assertNotNull(groupUid);
            }
            {
                // Create a user with 2 groups, and specify a default group
                // Verify that the attributes are correctly set on the retrieved user
                //
                Set<Attribute> attrs = fillInSampleUser(TEST_USER2);
                // Specify groups and default group
                //
                attrs.add(AttributeBuilder.build(getDefaultGroupName(), TEST_GROUP1_UID.getUidValue()));
                List<String> groups = new LinkedList<String>();
                List<String> owners = new LinkedList<String>();
                List<String> allGroups = new LinkedList<String>();
                allGroups.add(TEST_GROUP1_UID.getUidValue().toUpperCase());
                allGroups.add(TEST_GROUP2_UID.getUidValue().toUpperCase());
                groups.add(TEST_GROUP2_UID.getUidValue());
                owners.add(makeUid(SYSTEM_USER, ObjectClass.ACCOUNT).getUidValue());
                
                // Groups should not include the default group
                //
                attrs.add(AttributeBuilder.build(getGroupsAttributeName(), groups));
                attrs.add(AttributeBuilder.build(getGroupConnOwnersAttributeName(), owners));
                Uid userUid = connector.create(ObjectClass.ACCOUNT, attrs, options);
                
                ConnectorObject user = getUser(makeUid(TEST_USER2, ObjectClass.ACCOUNT).getUidValue(), connector);
                Attribute groupsAttr = user.getAttributeByName(getGroupsAttributeName());
                Assert.assertNotNull(groupsAttr);
                List<Object> retrievedGroups = groupsAttr.getValue();
                Assert.assertTrue(retrievedGroups.size()==2);
                Assert.assertTrue(allGroups.contains(((String)retrievedGroups.get(0)).toUpperCase()));
                Assert.assertTrue(allGroups.contains(((String)retrievedGroups.get(1)).toUpperCase()));
    
                Attribute defaultGroupAttr = user.getAttributeByName(getDefaultGroupName());
                Assert.assertNotNull(defaultGroupAttr);
                List<Object> defaultGroupAttrValue = defaultGroupAttr.getValue();
                Assert.assertEquals(defaultGroupAttrValue.get(0).toString().toUpperCase(), TEST_GROUP1_UID.getUidValue().toUpperCase());
            }
            {
                ConnectorObject user = getUser(makeUid(TEST_USER2, ObjectClass.ACCOUNT).getUidValue(), connector);
                // Move the user from group2 to group3
                //
                Set<Attribute> attrs = new HashSet<Attribute>();
                List<String> groups = new LinkedList<String>();
                List<String> owners = new LinkedList<String>();
                groups.add(TEST_GROUP3_UID.getUidValue());
                owners.add(makeUid(SYSTEM_USER, ObjectClass.ACCOUNT).getUidValue());
                attrs.add(AttributeBuilder.build(getGroupsAttributeName(), groups));
                attrs.add(AttributeBuilder.build(getGroupConnOwnersAttributeName(), owners));
                attrs.add(user.getUid());
                connector.update(ObjectClass.ACCOUNT, attrs, options);
                
                user = getUser(makeUid(TEST_USER2, ObjectClass.ACCOUNT).getUidValue(), connector);
                Attribute groupsAttr = user.getAttributeByName(getGroupsAttributeName());
                Assert.assertNotNull(groupsAttr);
                SortedSet<String> retrievedGroups = CollectionUtil.newCaseInsensitiveSet();
                for (Object value : groupsAttr.getValue()) 
                    retrievedGroups.add((String)value);
                Assert.assertTrue(retrievedGroups.size()==2);
                Assert.assertTrue(retrievedGroups.contains(TEST_GROUP3_UID.getUidValue().toUpperCase()));
    
                Attribute defaultGroupAttr = user.getAttributeByName(getDefaultGroupName());
                Assert.assertNotNull(defaultGroupAttr);
                List<Object> defaultGroupAttrValue = defaultGroupAttr.getValue();
                Assert.assertEquals(defaultGroupAttrValue.get(0).toString().toUpperCase(), TEST_GROUP1_UID.getUidValue().toUpperCase());
            }
            
            deleteUser(TEST_USER_UID2, connector);
            deleteGroup(TEST_GROUP1_UID, connector);
            deleteGroup(TEST_GROUP2_UID, connector);
            deleteGroup(TEST_GROUP3_UID, connector);
        } finally {
            connector.dispose();
        }
    }

    @Test//@Ignore
    public void testDelete() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
    
            deleteUser(TEST_USER_UID, connector);
            try {
                Uid newUid = connector.create(ObjectClass.ACCOUNT, attrs, null);
                System.out.println(newUid.getValue()+" created");
            } catch (RuntimeException rte) {
                if (!(rte.getCause() instanceof AlreadyExistsException)) {
                    throw rte;
                } 
            }
    
            // Delete the account
            //
            deleteUser(TEST_USER_UID, connector);
            System.out.println(TEST_USER_UID.getValue()+" deleted");
            
            // Second delete should fail
            //
            try {
                connector.delete(ObjectClass.ACCOUNT, TEST_USER_UID, null);
                Assert.fail("should have thrown");
            } catch (UnknownUidException uue) {
                // expected
            }
            System.out.println(TEST_USER_UID.getValue()+" deleted");
        } finally {
            connector.dispose();
        }
    }

    protected Set<Attribute> fillInSampleUser(final String testUser) {
        Set<Attribute> attrs = new HashSet<Attribute>();
        attrs.add(new Name(makeUid(testUser, ObjectClass.ACCOUNT).getUidValue()));
        attrs.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, new GuardedString("password".toCharArray())));
        attrs.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRED_NAME, Boolean.FALSE));
        //attrs.add(AttributeBuilder.build("objectclass", "racfUser"));
        attrs.add(AttributeBuilder.build(getInstallationDataAttributeName(), "original data"));
        return attrs;
    }

    protected RacfConnector createConnector(RacfConfiguration config)
    throws Exception {
        RacfConnector connector = new RacfConnector();
        connector.init(config);
        return connector;
    }

    protected RacfConfiguration createConfiguration() throws IOException {
        RacfConfiguration config = new RacfConfiguration();
        initializeLdapConfiguration(config);
        initializeCommandLineConfiguration(config);
        config.validate();
        //TODO: tests expect suffix as part of name
        if (config.getSuffix()==null)
            config.setSuffix(SUFFIX);
        OurConnectorMessages messages = new OurConnectorMessages();
        Map<Locale, Map<String, String>> catalogs = new HashMap<Locale, Map<String,String>>();
        Map<String, String> foo = new HashMap<String, String>();
        addBundle(foo, "org.identityconnectors.racf.Messages");
        addBundle(foo, "org.identityconnectors.racf.Messages"); 
        addBundle(foo, "org.identityconnectors.rw3270.Messages"); 
        addBundle(foo, "org.identityconnectors.rw3270.hod.Messages");
        catalogs.put(Locale.getDefault(), foo);
        messages.setCatalogs(catalogs);
        config.setConnectorMessages(messages);
        
        return config;
    }

    private void addBundle(Map<String, String> foo, String bundle) {
        ResourceBundle messagesBundle = ResourceBundle.getBundle(bundle);
        Enumeration<String> enumeration = messagesBundle.getKeys();
        while (enumeration.hasMoreElements()) {
            String key = enumeration.nextElement();
            foo.put(key, messagesBundle.getString(key));
        }
    }

    private RacfConfiguration createUserConfiguration() throws IOException {
        RacfConfiguration config = createConfiguration();
        config.setPassword(new GuardedString("password".toCharArray()));
        config.setUserName(TEST_USER);
        return config;
    }
    
    public static class TestHandler implements ResultsHandler, Iterable<ConnectorObject> {
        private List<ConnectorObject> objects = new LinkedList<ConnectorObject>();

        public boolean handle(ConnectorObject object) {
            objects.add(object);
            return true;
        }

        public Iterator<ConnectorObject> iterator() {
            return objects.iterator();
        }

        public int size() {
            return objects.size();
        }
    }

    public static class OurConnectorMessages implements ConnectorMessages {
        private Map<Locale, Map<String, String>> _catalogs = new HashMap<Locale, Map<String, String>>();

        public String format(String key, String defaultValue, Object... args) {
            Locale locale = CurrentLocale.isSet()?CurrentLocale.get():Locale.getDefault();
            Map<String,String> catalog = _catalogs.get(locale);
            String message = catalog.get(key);
            MessageFormat formatter = new MessageFormat(message,locale);
            return formatter.format(args, new StringBuffer(), null).toString();
        }

        public void setCatalogs(Map<Locale,Map<String,String>> catalogs) {
            _catalogs = catalogs;
        }
    }
    
    // Override these to do Ldap tests
    //
    protected abstract void initializeCommandLineConfiguration(RacfConfiguration config) throws IOException;
    protected abstract void initializeLdapConfiguration(RacfConfiguration config);
    protected abstract String getInstallationDataAttributeName();
    protected abstract String getDefaultGroupName();
    protected abstract String getAttributesAttributeName();
    protected abstract String getOwnerAttributeName();
    protected abstract String getSupgroupAttributeName();
    protected abstract String getGroupMembersAttributeName();
    protected abstract String getGroupsAttributeName();
    protected abstract String getGroupConnOwnersAttributeName();
    protected abstract String getTsoSizeName();
    protected abstract Uid makeUid(String name, ObjectClass objectClass);
}
