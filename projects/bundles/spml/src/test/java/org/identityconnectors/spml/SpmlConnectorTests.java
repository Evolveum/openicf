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
package org.identityconnectors.spml;

import java.text.MessageFormat;
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

import junit.framework.Assert;

import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
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
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.OrFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.BeforeClass;
import org.junit.Test;


public class SpmlConnectorTests {

    // Connector Configuration information
    //
    private static final String URL                   = "http://idmvm2026.central.sun.com:8080/idm/servlet/openspml2";
    private static final String SPML_OBJ_CLASS        = "spml2Person";
    private static final String CONNECTOR_OBJ_CLASS   = ObjectClass.ACCOUNT_NAME;
    private static final String PSO_TARGET_CLASS      = "spml2-DSML-Target";
    private static final String ACCOUNT_ID            = "accountId";

    private static final String ATTR_FIRSTNAME        = "firstname";
    private static final String ATTR_LASTNAME         = "lastname";
    private static final String ATTR_FULLNAME         = "fullname";

    private static String HOST_NAME;
    private static String SYSTEM_PASSWORD;
    private static String SYSTEM_USER;
    private static String TEST_USER;
    
    public SpmlConnectorTests() {
    }

    @Test
    public void testNullsInConfig() throws Exception {
        {
            SpmlConfiguration config = new SpmlConfiguration();
            config.getObjectClassNames();
            // Validate that setting this to null doesn't error out
            //
            config.setObjectClassNames(null);
            config.getObjectClassNames();
        }
        {
            SpmlConfiguration config = new SpmlConfiguration();
            config.getConnectorMessages();
            // Validate that setting this to null doesn't error out
            //
            config.setConnectorMessages(null);
            config.getConnectorMessages();
        }
        {
            SpmlConfiguration config = new SpmlConfiguration();
            config.getUrl();
            // Validate that setting this to null doesn't error out
            //
            config.setUrl(null);
            config.getUrl();
        }
        {
            SpmlConfiguration config = new SpmlConfiguration();
            config.getNameAttributes();
            // Validate that setting this to null doesn't error out
            //
            config.setNameAttributes(null);
            config.getNameAttributes();
        }
        {
            SpmlConfiguration config = new SpmlConfiguration();
            config.getPassword();
            // Validate that setting this to null doesn't error out
            //
            config.setPassword(null);
            config.getPassword();
        }
        {
            SpmlConfiguration config = new SpmlConfiguration();
            config.getUserName();
            // Validate that setting this to null doesn't error out
            //
            config.setUserName(null);
            config.getUserName();
        }
        {
            SpmlConfiguration config = new SpmlConfiguration();
            config.getPreDisconnectCommand();
            // Validate that setting this to null doesn't error out
            //
            config.setPreDisconnectCommand(null);
            config.getPreDisconnectCommand();
        }
        {
            SpmlConfiguration config = new SpmlConfiguration();
            config.getPostConnectCommand();
            // Validate that setting this to null doesn't error out
            //
            config.setPostConnectCommand(null);
            config.getPostConnectCommand();
        }
        {
            SpmlConfiguration config = new SpmlConfiguration();
            config.getPreSendCommand();
            // Validate that setting this to null doesn't error out
            //
            config.setPreSendCommand(null);
            config.getPreSendCommand();
        }
        {
            SpmlConfiguration config = new SpmlConfiguration();
            config.getPostReceiveCommand();
            // Validate that setting this to null doesn't error out
            //
            config.setPostReceiveCommand(null);
            config.getPostReceiveCommand();
        }
        //Ensure that validation catches nul fields
        {
            try {
                SpmlConfiguration config = createConfiguration();
                config.setObjectClassNames(null);
                config.validate();
                Assert.fail("expected exception");
            } catch (RuntimeException rte) {
                // expected
            }
        }
        {
            try {
                SpmlConfiguration config = createConfiguration();
                config.setUrl(null);
                config.validate();
                Assert.fail("expected exception");
            } catch (RuntimeException rte) {
                // expected
            }
        }
        {
            try {
                SpmlConfiguration config = createConfiguration();
                config.setNameAttributes(null);
                config.validate();
                Assert.fail("expected exception");
            } catch (RuntimeException rte) {
                // expected
            }
        }
        {
            try {
                SpmlConfiguration config = createConfiguration();
                config.setPassword(null);
                config.validate();
                Assert.fail("expected exception");
            } catch (RuntimeException rte) {
                // expected
            }
        }
        {
            try {
                SpmlConfiguration config = createConfiguration();
                config.setUserName(null);
                config.validate();
                Assert.fail("expected exception");
            } catch (RuntimeException rte) {
                // expected
            }
        }
    }

    @BeforeClass
    public static void before() {
        PropertyBag testProps = TestHelpers.getProperties(SpmlConnector.class);
        HOST_NAME         = testProps.getStringProperty("HOST_NAME");
        SYSTEM_PASSWORD   = testProps.getStringProperty("SYSTEM_PASSWORD");
        SYSTEM_USER       = testProps.getStringProperty("SYSTEM_USER");
        TEST_USER         = "SPML101";
        Assert.assertNotNull("HOST_NAME must be specified", HOST_NAME);
        Assert.assertNotNull("SYSTEM_PASSWORD must be specified", SYSTEM_PASSWORD);
        Assert.assertNotNull("SYSTEM_USER must be specified", SYSTEM_USER);
    }

    @Test//@Ignore
    public void testListSchema() throws Exception {
        SpmlConfiguration config = createConfiguration();
        SpmlConnector info = createConnector(config);
        try {
            Schema schema = info.schema();
            boolean personFound = false;
            boolean firstnameFound = false;
            for (ObjectClassInfo ocInfo : schema.getObjectClassInfo()) {
                if (ocInfo.is(ObjectClass.ACCOUNT_NAME)) {
                    System.out.println("Schema for "+ocInfo.getType());
                    personFound = true;
                    for (AttributeInfo attr : ocInfo.getAttributeInfo()) {
                        System.out.println("    "+attr);
                        if (attr.getName().equals("firstname")) {
                            firstnameFound = true;
                        }
                    }
                }
            }
            Assert.assertTrue(personFound);
            Assert.assertTrue(firstnameFound);
        } finally {
            info.dispose();
        }
    }

    @Test//@Ignore
    public void testListAllUsers() throws Exception {
        SpmlConfiguration config = createConfiguration();
        SpmlConnector info = createConnector(config);
        try {
            TestHandler handler = new TestHandler();
            TestHelpers.search(info,ObjectClass.ACCOUNT, null, handler, null);
            for (ConnectorObject user : handler) {
                System.out.println("Read User:"+user.getUid().getValue());
            }
        } finally {
            info.dispose();
        }
    }

    @Test//@Ignore
    public void testGetSpecifiedUser() throws Exception {
        SpmlConfiguration config = createConfiguration();
        SpmlConnector info = createConnector(config);
        try {
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
    
            deleteUser(TEST_USER, info);
    
            info.create(ObjectClass.ACCOUNT, attrs, null);
    
            ConnectorObject user = getUser(TEST_USER);
            Assert.assertNotNull(user);
            
            TestHandler handler = new TestHandler();
            TestHelpers.search(info,ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(Uid.NAME, "asdhjfdaslfh alsk fhasldk ")), handler, null);
            Assert.assertFalse(handler.iterator().hasNext());

            handler = new TestHandler();
            String[] attributesToGet = { "firstname" };
            Map<String, Object> optionsMap = new HashMap<String, Object>();
            optionsMap.put(OperationOptions.OP_ATTRIBUTES_TO_GET, attributesToGet); 
            OperationOptions options = new OperationOptions(optionsMap);
            TestHelpers.search(info,ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(Uid.NAME, TEST_USER)), handler, options);
            Assert.assertTrue(handler.iterator().hasNext());
            ConnectorObject object = handler.iterator().next();
            Assert.assertNotNull(object.getAttributeByName("firstname"));
            Assert.assertNull(object.getAttributeByName("lastname"));
        } finally {
            info.dispose();
        }
    }

    @Test//@Ignore
    public void testTest() throws Exception {
        SpmlConfiguration config = createConfiguration();
        SpmlConnector info = createConnector(config);
        try {
            info.checkAlive();
        } finally {
            info.dispose();
        }
    }

    @Test//@Ignore
    public void testSearchSpecifiedUser() throws Exception {
        SpmlConfiguration config = createConfiguration();
        SpmlConnector info = createConnector(config);
        try {
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
    
            deleteUser(TEST_USER, info);
            Uid createdUserUid = info.create(ObjectClass.ACCOUNT, attrs, null);
            
            // Simple test of EqualsFilter
            //
            TestHandler handler = new TestHandler();
            TestHelpers.search(info,ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(ATTR_LASTNAME, "User")), handler, null);
            boolean found = false;
            int count = 0;
            for (ConnectorObject user : handler) {
                if (TEST_USER.equals(user.getName().getNameValue()))
                    found = true;
                count++;
            }
            Assert.assertTrue(count==1);
            Assert.assertTrue(found);
                        
            // Simple test of StartsWithFilter
            //
            handler = new TestHandler();
            TestHelpers.search(info,ObjectClass.ACCOUNT, new StartsWithFilter(AttributeBuilder.build(ATTR_LASTNAME, "User")), handler, null);
            found = false;
            for (ConnectorObject user : handler) {
                if (TEST_USER.equals(user.getName().getNameValue()))
                    found = true;
                Assert.assertTrue(AttributeUtil.getStringValue(user.getAttributeByName(ATTR_LASTNAME)).startsWith("User"));
            }
            Assert.assertTrue(found);
                        
            // Simple test of EndsWithFilter
            //
            handler = new TestHandler();
            TestHelpers.search(info,ObjectClass.ACCOUNT, new EndsWithFilter(AttributeBuilder.build(ATTR_LASTNAME, "User")), handler, null);
            found = false;
            for (ConnectorObject user : handler) {
                if (TEST_USER.equals(user.getName().getNameValue()))
                    found = true;
                Assert.assertTrue(AttributeUtil.getStringValue(user.getAttributeByName(ATTR_LASTNAME)).endsWith("User"));
            }
            Assert.assertTrue(found);
                        
            // Simple test of ContainsFilter
            //
            handler = new TestHandler();
            TestHelpers.search(info,ObjectClass.ACCOUNT, new ContainsFilter(AttributeBuilder.build(ATTR_LASTNAME, "User")), handler, null);
            found = false;
            for (ConnectorObject user : handler) {
                if (TEST_USER.equals(user.getName().getNameValue()))
                    found = true;
                Assert.assertTrue(AttributeUtil.getStringValue(user.getAttributeByName(ATTR_LASTNAME)).contains("User"));
            }
            Assert.assertTrue(found);
                        
            // Simple test of EqualsFilter
            //
            {
	            handler = new TestHandler();
	            TestHelpers.search(info,ObjectClass.ACCOUNT, new EqualsFilter(createdUserUid), handler, null);
	            found = false;
	            count = 0;
	            for (ConnectorObject user : handler) {
	                if (TEST_USER.equals(user.getName().getNameValue()))
	                    found = true;
	                count++;
	            }
	            Assert.assertTrue(count==1);
	            Assert.assertTrue(found);
            }
            
            // Test of And
            //
            handler = new TestHandler();
            Filter filter = new AndFilter(
                    new EqualsFilter(AttributeBuilder.build(ATTR_LASTNAME, "User")),
                    new EqualsFilter(AttributeBuilder.build(ATTR_FIRSTNAME, "SPML")));
            TestHelpers.search(info,ObjectClass.ACCOUNT, filter, handler, null);
            found = false;
            count = 0;
            for (ConnectorObject user : handler) {
                if (TEST_USER.equals(user.getName().getNameValue()))
                    found = true;
                count++;
            }
            
            Assert.assertTrue(count==1);
            Assert.assertTrue(found);
            
            // Change the first name
            //
            ConnectorObject updateUser = getUser(TEST_USER);
            Set<Attribute> changed = new HashSet<Attribute>();
            changed.add(AttributeBuilder.build(ATTR_FIRSTNAME, "abel"));
            changed.add(updateUser.getUid());
            info.update(ObjectClass.ACCOUNT, changed, null);

            // Test of And, which should fail, since firstname has changed
            //
            handler = new TestHandler();
            filter = new AndFilter(
                    new EqualsFilter(AttributeBuilder.build(ATTR_LASTNAME, "User")),
                    new EqualsFilter(AttributeBuilder.build(ATTR_FIRSTNAME, "SPML")));
            TestHelpers.search(info,ObjectClass.ACCOUNT, filter, handler, null);
            found = false;
            count = 0;
            for (ConnectorObject user : handler) {
                if (TEST_USER.equals(user.getName().getNameValue()))
                    found = true;
                count++;
            }
            Assert.assertTrue(count==0);
            Assert.assertTrue(!found);

            // Test of Or, which should succeed, since lastname has not changed
            //
            handler = new TestHandler();
            filter = new OrFilter(
                    new EqualsFilter(AttributeBuilder.build(ATTR_LASTNAME, "User")),
                    new EqualsFilter(AttributeBuilder.build(ATTR_FIRSTNAME, "SPML")));
            TestHelpers.search(info,ObjectClass.ACCOUNT, filter, handler, null);
            found = false;
            count = 0;
            for (ConnectorObject user : handler) {
                if (TEST_USER.equals(user.getName().getNameValue()))
                    found = true;
                count++;
            }
            Assert.assertTrue(count>0);
            Assert.assertTrue(found);

        } finally {
            info.dispose();
        }
    }

    @Test//@Ignore
    public void testModifyUser() throws Exception {
        SpmlConfiguration config = createConfiguration();
        SpmlConnector info = createConnector(config);

        try {
            // Delete the user
            //
            deleteUser(TEST_USER, info);
    
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
            info.create(ObjectClass.ACCOUNT, attrs, null);
    
            ConnectorObject user = getUser(TEST_USER);
            Set<Attribute> changed = new HashSet<Attribute>();
            changed.add(AttributeBuilder.build(ATTR_FIRSTNAME, "abel"));
            changed.add(user.getUid());
            info.update(ObjectClass.ACCOUNT, changed, null);
    
            ConnectorObject changedUser = getUser(TEST_USER);
            Attribute firstname = changedUser.getAttributeByName(ATTR_FIRSTNAME);
            displayUser(changedUser);
            Assert.assertNotNull(firstname);
            Assert.assertTrue(AttributeUtil.getStringValue(firstname).equalsIgnoreCase("abel"));
        } finally {
            info.dispose();
        }
    }

    private void displayUser(ConnectorObject user) {
        Set<Attribute> attributes = user.getAttributes();
        for (Attribute attribute : attributes) {
            System.out.println(attribute.getName());
            List<Object> values = attribute.getValue();
            for (Object value : values) {
                System.out.println("    "+value.getClass().getName()+":"+value);
            }
        }
    }

    private ConnectorObject getUser(String accountId) throws Exception  {
        return getUser(accountId, null);
    }

    private ConnectorObject getUser(String accountId, OperationOptions options) throws Exception  {
        SpmlConfiguration config = createConfiguration();
        SpmlConnector info = createConnector(config);

        try {
            TestHandler handler = new TestHandler();
            TestHelpers.search(info,ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(Name.NAME, accountId)), handler, options);
            if (!handler.iterator().hasNext())
                return null;
            return handler.iterator().next();
        } catch (UnknownUidException e) {
            return null;
        } finally {
            info.dispose();
        }
    }

    @Test
    public void testNegative() throws Exception {
        SpmlConfiguration config = createConfiguration();
        config.setPassword(new GuardedString("bogus".toCharArray()));
        try {
            createConnector(config);
            Assert.fail("expected exception");
        } catch (RuntimeException e) {
            // expected
        }
        config = createConfiguration();
        config.setPostConnectCommand(null);
        SpmlConnector info = createConnector(config);
        try {
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
            info.create(ObjectClass.ACCOUNT, attrs, null);
            Assert.fail("expected exception");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            info.delete(ObjectClass.ACCOUNT, new Uid(TEST_USER), null);
            Assert.fail("expected exception");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
            info.update(ObjectClass.ACCOUNT, attrs, null);
            Assert.fail("expected exception");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            TestHandler handler = new TestHandler();
            TestHelpers.search(info,ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(Uid.NAME, "asdhjfdaslfh alsk fhasldk ")), handler, null);
            Assert.fail("expected exception");
        } catch (RuntimeException e) {
            // expected
        }

    }

    @Test//@Ignore
    public void testCreate() throws Exception {
        SpmlConfiguration config = createConfiguration();
        SpmlConnector info = createConnector(config);
        Set<Attribute> attrs = fillInSampleUser(TEST_USER);

        try {
            // Delete the account if it already exists
            //
            deleteUser(TEST_USER, info);
    
            // Create the account
            //
            Uid newUid = info.create(ObjectClass.ACCOUNT, attrs, null);
            System.out.println(newUid.getValue()+" created");
            try {
                info.create(ObjectClass.ACCOUNT, attrs, null);
                Assert.fail("should have thrown exception");
            } catch (AlreadyExistsException aee) {
                // expected
            }
        } finally {
            info.dispose();
        }
    }

    @Test//@Ignore
    public void testChangePassword() throws Exception {
        SpmlConfiguration config = createConfiguration();
        SpmlConnector info = createConnector(config);
        try {
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
    
            // Delete the account if it already exists
            //
            deleteUser(TEST_USER, info);
    
            // Create the account
            //
            Uid newUid = info.create(ObjectClass.ACCOUNT, attrs, null);
            System.out.println(newUid.getValue()+" created");
    
            ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
            builder.setUid(newUid);
            Attribute password = AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, new GuardedString("xyzzy123".toCharArray()));
            builder.addAttribute(password);
            builder.addAttribute(new Name(TEST_USER));
            ConnectorObject newUser = builder.build();
            info.update(newUser.getObjectClass(), newUser.getAttributes(), null);
        } finally {
            info.dispose();
        }
    }

    @Test//@Ignore
    public void testEnableDisable() throws Exception {
        SpmlConfiguration config = createConfiguration();
        SpmlConnector info = createConnector(config);
        try {
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
    
            // Delete the account if it already exists
            //
            deleteUser(TEST_USER, info);
    
            // Create the account
            //
            Uid newUid = info.create(ObjectClass.ACCOUNT, attrs, null);
            System.out.println(newUid.getValue()+" created");
            
            // Test disabling the user
            { 
                ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
                builder.setUid(newUid);
                Attribute password = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, Boolean.FALSE);
                builder.addAttribute(password);
                builder.addAttribute(new Name(TEST_USER));
                ConnectorObject newUser = builder.build();
                info.update(newUser.getObjectClass(), newUser.getAttributes(), null);
                
                Map map = new HashMap<String, Object>();
                map.put(OperationOptions.OP_ATTRIBUTES_TO_GET, new String[] {OperationalAttributes.ENABLE_NAME});
                OperationOptions options = new OperationOptions(map);
                
                ConnectorObject user = getUser(TEST_USER, options);
                Attribute enabled = user.getAttributeByName(OperationalAttributes.ENABLE_NAME);
                Assert.assertNotNull(enabled);
                Assert.assertFalse(AttributeUtil.getBooleanValue(enabled));
            }
            
            // Test enabling the user
            { 
                ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
                builder.setUid(newUid);
                Attribute password = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, Boolean.TRUE);
                builder.addAttribute(password);
                builder.addAttribute(new Name(TEST_USER));
                ConnectorObject newUser = builder.build();
                info.update(newUser.getObjectClass(), newUser.getAttributes(), null);
                
                Attribute enabled = newUser.getAttributeByName(OperationalAttributes.ENABLE_NAME);
                Assert.assertNotNull(enabled);
                Assert.assertTrue(AttributeUtil.getBooleanValue(enabled));
            }
        } finally {
            info.dispose();
        }
    }

    private void deleteUser(final String testUser, SpmlConnector connector) {
        try {
            connector.delete(ObjectClass.ACCOUNT, new Uid("person:"+testUser), null);
        } catch (UnknownUidException rte) {
            // Ignore
        }
    }

    @Test//@Ignore
    public void testDelete() throws Exception {
        SpmlConfiguration config = createConfiguration();
        SpmlConnector info = createConnector(config);
        Set<Attribute> attrs = fillInSampleUser(TEST_USER);

        try {
            // Create the account if it doesn't already exist
            //
            ConnectorObject user = getUser(TEST_USER);
            Uid newUid= null;
            if (user==null) {
                try {
                    newUid = info.create(ObjectClass.ACCOUNT, attrs, null);
                    System.out.println(newUid.getValue()+" created");
                } catch (AlreadyExistsException rte) {
                    // ignore
                }
            } else {
                newUid = user.getUid();
            }
    
            // Delete the account
            //
            info.delete(ObjectClass.ACCOUNT, newUid, null);
            System.out.println(newUid.getValue()+" deleted");
            
            try {
                info.delete(ObjectClass.ACCOUNT, newUid, null);
                Assert.fail("Should have seen exception");
            } catch (UnknownUidException uue) {
                // expected
            }
        } finally {
            info.dispose();
        }
    }
    @Test//@Ignore
    public void testResolve() throws Exception {
        SpmlConfiguration config = createConfiguration();
        SpmlConnector info = createConnector(config);
        try {
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
    
            // Delete the account if it already exists
            //
            deleteUser(TEST_USER, info);
            try {
                info.resolveUsername(ObjectClass.ACCOUNT, TEST_USER, new OperationOptions(new HashMap()));
                Assert.fail("exception expected");
            } catch (UnknownUidException ue) {
                // expected
            }
    
            // Create the account
            //
            Uid newUid = info.create(ObjectClass.ACCOUNT, attrs, null);
            System.out.println(newUid.getValue()+" created");
            Uid retrievedUid = info.resolveUsername(ObjectClass.ACCOUNT, TEST_USER, new OperationOptions(new HashMap()));
            Assert.assertEquals(newUid, retrievedUid);
        } finally {
            info.dispose();
        }
    }

    private Set<Attribute> fillInSampleUser(final String testUser) {
        Set<Attribute> attrs = new HashSet<Attribute>();
        attrs.add(new Name(TEST_USER));
        attrs.add(AttributeBuilder.build(ATTR_FIRSTNAME, "SPML"));
        attrs.add(AttributeBuilder.build(ATTR_LASTNAME, "User"));
        attrs.add(AttributeBuilder.build(ATTR_FULLNAME, "SMPL User"));
        attrs.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, new GuardedString("xyzzy".toCharArray())));
        return attrs;
    }

    private SpmlConnector createConnector(SpmlConfiguration config)
    throws Exception {
        SpmlConnector rv = new SpmlConnector();
        rv.init(config);
        return rv;
    }

    private SpmlConfiguration createConfiguration() {
        SpmlConfiguration config = new SpmlConfiguration(URL, new String[] {CONNECTOR_OBJ_CLASS}, new String[] {SPML_OBJ_CLASS}, new String[] {PSO_TARGET_CLASS}, new String[] {ACCOUNT_ID}, SYSTEM_USER, new GuardedString(SYSTEM_PASSWORD.toCharArray()));
        config.setPreSendCommand(getPreSendCommand());
        config.setPostReceiveCommand(getPostReceiveCommand());
        config.setPostConnectCommand(getPostConnectCommand());
        config.setPreDisconnectCommand(getPreDisConnectCommand());
        config.setMapAttributeCommand(getMapAttributeCommand());
        config.setMapSetNameCommand(getMapSetNameCommand());
        config.setMapQueryNameCommand(getMapQueryNameCommand());
        config.setSchemaCommand(getSchemaCommand());
        config.setScriptingLanguage("GROOVY");

        OurConnectorMessages messages = new OurConnectorMessages();
        Map<Locale, Map<String, String>> catalogs = new HashMap<Locale, Map<String,String>>();
        Map<String, String> foo = new HashMap<String, String>();
        ResourceBundle messagesBundle = ResourceBundle.getBundle("org.identityconnectors.spml.Messages");
        Enumeration<String> enumeration = messagesBundle.getKeys();
        while (enumeration.hasMoreElements()) {
            String key = enumeration.nextElement();
            foo.put(key, messagesBundle.getString(key));
        }
        catalogs.put(Locale.getDefault(), foo);
        messages.setCatalogs(catalogs);
        config.setConnectorMessages(messages);
        
        return config;
    }

    private String getPostConnectCommand() {
        StringBuffer buffer = new StringBuffer();
        addGuardedStringAccessor(buffer);
        buffer.append("request = new org.openspml.v2.msg.spml.ListTargetsRequest();\n");
        buffer.append("request.addOpenContentElement(new org.openspml.v2.util.xml.OperationalNameValuePair(\"accountId\", username));\n");
        buffer.append("request.addOpenContentElement(new org.openspml.v2.util.xml.OperationalNameValuePair(\"password\", passwordString));\n");
        buffer.append("request.setExecutionMode(org.openspml.v2.msg.spml.ExecutionMode.SYNCHRONOUS);\n");
        buffer.append("response = connection.send(request);\n");
        buffer.append("oces = response.getOpenContentElements();\n");
        buffer.append("memory.session = oces[0]\n");
        return buffer.toString();
    }

    private String getPreDisConnectCommand() {
        StringBuffer buffer = new StringBuffer();
        addGuardedStringAccessor(buffer);
        buffer.append("if (!passwordString.equals(\"configurator\")) throw new RuntimeException(\"disconnect failure\");");
        return buffer.toString();
    }

    private String getPostReceiveCommand() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("if (response==null) throw new RuntimeException(\"post receive failure\");");
        return buffer.toString();
    }

    private String getPreSendCommand() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("if (memory.session!=null)request.addOpenContentElement(memory.session);\n");
        buffer.append("if (request instanceof org.openspml.v2.msg.spml.AddRequest) request.getData().addOpenContentElement(new org.openspml.v2.profiles.dsml.DSMLAttr(\"type\", \"User\"));");
        return buffer.toString();
    }

    private String getMapSetNameCommand() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("if (org.identityconnectors.framework.common.objects.Name.NAME.equals(name))\n");
        buffer.append("    return getNameAttribute(configuration, objectClass);\n");
        buffer.append("if (org.identityconnectors.framework.common.objects.OperationalAttributes.PASSWORD_NAME.equals(name))\n");
        buffer.append("    return \"credentials\";\n");
        buffer.append("return name;\n");
        buffer.append("private String getNameAttribute(org.identityconnectors.spml.SpmlConfiguration configuration, String objectClass) {\n");
        buffer.append("    for (int i=0; i<configuration.getObjectClassNames().length; i++) \n");
        buffer.append("        if (configuration.getObjectClassNames()[i].equals(objectClass))\n");
        buffer.append("            return configuration.getNameAttributes()[i];\n");
        buffer.append("    return \"\";\n");
        buffer.append("}\n");
        return buffer.toString();
    }
    
    private String getMapAttributeCommand() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("if (attribute.getName().equalsIgnoreCase(\"credentials\"))\n");
        buffer.append("    return org.identityconnectors.framework.common.objects.AttributeBuilder.buildPassword(new org.identityconnectors.common.security.GuardedString(((String)attribute.getValue().get(0)).toCharArray()));\n");
        buffer.append("else if (attribute.getName().equalsIgnoreCase(getNameAttribute(configuration, objectClass)))\n");
        buffer.append("    return new org.identityconnectors.framework.common.objects.Name((String)attribute.getValue().get(0));\n");
        buffer.append("return attribute;");
        buffer.append("private String getNameAttribute(org.identityconnectors.spml.SpmlConfiguration configuration, String objectClass) {\n");
        buffer.append("    for (int i=0; i<configuration.getObjectClassNames().length; i++) \n");
        buffer.append("        if (configuration.getObjectClassNames()[i].equals(objectClass))\n");
        buffer.append("            return configuration.getNameAttributes()[i];\n");
        buffer.append("    return \"\";\n");
        buffer.append("}\n");
        return buffer.toString();
    }

    private String getMapQueryNameCommand() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("if (org.identityconnectors.framework.common.objects.OperationalAttributes.PASSWORD_NAME.equals(name))\n");
        buffer.append("    return \"credentials\";\n");
        buffer.append("else if (org.identityconnectors.framework.common.objects.Name.NAME.equals(name))\n");
        buffer.append("    return \"name\";\n");
        buffer.append("return name;");
        return buffer.toString();
    }

    private String getSchemaCommand() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("if (org.identityconnectors.framework.common.objects.ObjectClass.ACCOUNT_NAME.equals(objectClass)) {\n");
        buffer.append("    for (org.identityconnectors.framework.common.objects.AttributeInfo info : attributeInfos) {\n");
        buffer.append("	       if (info.getName().equals(\"credentials\")) {\n");
        buffer.append("            attributeInfos.remove(info);\n");
        buffer.append("            break;\n");
        buffer.append("        }\n");
        buffer.append("    }\n");
        buffer.append("    for (org.identityconnectors.framework.common.objects.AttributeInfo info : attributeInfos) {\n");
        buffer.append("        if (info.getName().equals(\"accountId\")) {\n");
        buffer.append("            attributeInfos.remove(info);\n");
        buffer.append("            break;\n");
        buffer.append("        }\n");
        buffer.append("    }\n");
        buffer.append("    for (org.identityconnectors.framework.common.objects.AttributeInfo info : attributeInfos) {\n");
        buffer.append("        if (info.getName().equals(\"__ENABLE__\")) {\n");
        buffer.append("            attributeInfos.remove(info);\n");
        buffer.append("            break;\n");
        buffer.append("        }\n");
        buffer.append("    }\n");
        buffer.append("    attributeInfos.add(org.identityconnectors.framework.common.objects.OperationalAttributeInfos.PASSWORD);\n");
        buffer.append("    attributeInfos.add(asNotByDefault(org.identityconnectors.framework.common.objects.OperationalAttributeInfos.ENABLE));\n");
        buffer.append("}\n");
        buffer.append("private org.identityconnectors.framework.common.objects.AttributeInfo asWriteOnly(org.identityconnectors.framework.common.objects.AttributeInfo original) {\n");
        buffer.append("    org.identityconnectors.framework.common.objects.AttributeInfoBuilder builder = new org.identityconnectors.framework.common.objects.AttributeInfoBuilder();\n");
        buffer.append("    builder.setMultiValued(original.isMultiValued());\n");
        buffer.append("    builder.setName(original.getName());\n");
        buffer.append("    builder.setReadable(original.isReadable());\n");
        buffer.append("    builder.setRequired(original.isRequired());\n");
        buffer.append("    builder.setReturnedByDefault(false);\n");
        buffer.append("    builder.setType(original.getType());\n");
        buffer.append("    builder.setCreateable(false);\n");
        buffer.append("    builder.setUpdateable(false);\n");
        buffer.append("    return builder.build();\n");
        buffer.append("}    \n");
        buffer.append("\n");
        buffer.append("private org.identityconnectors.framework.common.objects.AttributeInfo asNotByDefault(org.identityconnectors.framework.common.objects.AttributeInfo original) {\n");
        buffer.append("    org.identityconnectors.framework.common.objects.AttributeInfoBuilder builder = new org.identityconnectors.framework.common.objects.AttributeInfoBuilder();\n");
        buffer.append("    builder.setMultiValued(original.isMultiValued());\n");
        buffer.append("    builder.setName(original.getName());\n");
        buffer.append("    builder.setReadable(original.isReadable());\n");
        buffer.append("    builder.setRequired(original.isRequired());\n");
        buffer.append("    builder.setReturnedByDefault(false);\n");
        buffer.append("    builder.setType(original.getType());\n");
        buffer.append("    builder.setCreateable(original.isCreateable());\n");
        buffer.append("    builder.setUpdateable(original.isUpdateable());\n");
        buffer.append("    return builder.build();\n");
        buffer.append("}\n");
        return buffer.toString();
    }
    
    private void addGuardedStringAccessor(StringBuffer buffer) {
        buffer.append("class GuardedStringAccessor implements org.identityconnectors.common.security.GuardedString.Accessor {\n");
        buffer.append("    private char[] _array;\n");
        buffer.append("    public void access(char[] clearChars) {\n");
        buffer.append("        _array = new char[clearChars.length];\n");
        buffer.append("        System.arraycopy(clearChars, 0, _array, 0, _array.length);            \n");
        buffer.append("    }\n");
        buffer.append("    public char[] getArray() {\n");
        buffer.append("        return _array;\n");
        buffer.append("    }\n");
        buffer.append("    public void clear() {\n");
        buffer.append("        //Arrays.fill(_array, 0, _array.length, ' ');\n");
        buffer.append("    }\n");
        buffer.append("}\n");
        buffer.append("accessor = new GuardedStringAccessor();\n");
        buffer.append("password.access(accessor);\n");
        buffer.append("passwordString = new String(accessor.getArray());\n");
        buffer.append("accessor.clear();\n");
    }

    public class OurConnectorMessages implements ConnectorMessages {
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
    
    public static class TestHandler implements ResultsHandler, Iterable<ConnectorObject> {
        private List<ConnectorObject> objects = new LinkedList<ConnectorObject>();

        public boolean handle(ConnectorObject object) {
            objects.add(object);
            return true;
        }

        public Iterator<ConnectorObject> iterator() {
            return objects.iterator();
        }
    }
}
