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
package org.identityconnectors.racf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.patternparser.MapTransform;
import org.identityconnectors.patternparser.Transform;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.identityconnectors.racf.RacfConstants.*;

public class RacfConnectorTests {
    // Connector Configuration information
    //
    private static String       HOST_NAME;
    private static String       SYSTEM_PASSWORD;
    private static String       SYSTEM_USER;
    private static String       SYSTEM_USER_LDAP;
    private static String       SUFFIX;
    private static String       TEST_USER = "TEST106";
    private static String       TEST_USER2 = "TEST106A";
    private static String       TEST_GROUP1 = "IDMGRP01";
    private static String       TEST_GROUP2 = "IDMGRP02";
    private static Uid          TEST_USER_UID;
    private static Uid          TEST_USER_UID2;
    private static Uid          TEST_GROUP1_UID;
    private static Uid          TEST_GROUP2_UID;

    private static final int     HOST_PORT           = 389;
    private static final int     HOST_TELNET_PORT    = 23;
    private static final Boolean USE_SSL             = Boolean.FALSE;
    
    private static final String GROUP_RACF_PARSER   = "org/identityconnectors/racf/GroupRacfSegmentParser.xml";
    private static final String RACF_PARSER         = "org/identityconnectors/racf/RacfSegmentParser.xml";
    private static final String CICS_PARSER         = "org/identityconnectors/racf/CicsSegmentParser.xml";
    private static final String OMVS_PARSER         = "org/identityconnectors/racf/OmvsSegmentParser.xml";
    private static final String TSO_PARSER          = "org/identityconnectors/racf/TsoSegmentParser.xml";
    private static final String NETVIEW_PARSER      = "org/identityconnectors/racf/NetviewSegmentParser.xml";
    private static final String CATALOG_PARSER      = "org/identityconnectors/racf/CatalogParser.xml";

    public static void main(String[] args) {
        RacfConnectorTests tests = new RacfConnectorTests();
        try {
            tests.testCreate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @BeforeClass
    public static void beforeClass() {
        HOST_NAME         = TestHelpers.getProperty("HOST_NAME", null);
        SYSTEM_PASSWORD   = TestHelpers.getProperty("SYSTEM_PASSWORD", null);
        SUFFIX            = TestHelpers.getProperty("SUFFIX", null);
        SYSTEM_USER       = TestHelpers.getProperty("SYSTEM_USER", null);
        
        SYSTEM_USER_LDAP  = "racfid="+SYSTEM_USER+",profileType=user,"+SUFFIX;
        TEST_USER_UID     = new Uid("racfid="+TEST_USER+",profileType=user,"+SUFFIX);
        TEST_USER_UID2    = new Uid("racfid="+TEST_USER2+",profileType=user,"+SUFFIX);
        TEST_GROUP1_UID   = new Uid("racfid="+TEST_GROUP1.toUpperCase()+",profileType=group,"+SUFFIX);
        TEST_GROUP2_UID   = new Uid("racfid="+TEST_GROUP2.toUpperCase()+",profileType=group,"+SUFFIX);
        
        Assert.assertNotNull("HOST_NAME must be specified", HOST_NAME);
        Assert.assertNotNull("SYSTEM_PASSWORD must be specified", SYSTEM_PASSWORD);
        Assert.assertNotNull("SYSTEM_USER must be specified", SYSTEM_USER);
        Assert.assertNotNull("SUFFIX must be specified", SUFFIX);
    }

    @Before
    public void before() {
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
            TestHelpers.search(connector,ObjectClass.GROUP, null, handler, null);
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
    public void testListAllUsersNameOnly() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            TestHandler handler = new TestHandler();
            Map map = new HashMap();
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
            Map map = new HashMap();
            String[] attributesToGet = { Name.NAME };
            map.put(OperationOptions.OP_ATTRIBUTES_TO_GET, attributesToGet);
            OperationOptions options = new OperationOptions(map);
            TestHelpers.search(connector,ObjectClass.GROUP, null, handler, options);
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
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
            
            // Create the account if it doesn't already exist
            //
            deleteUser(TEST_USER_UID, connector);
            connector.create(ObjectClass.ACCOUNT, attrs, null);
    
            boolean found = false;
            int count = 0;
            TestHandler handler = new TestHandler();
            Map<String, Object> optionsMap = new HashMap<String, Object>();
            optionsMap.put(OperationOptions.OP_ATTRIBUTES_TO_GET, new String[] {Name.NAME});
            OperationOptions options = new OperationOptions(optionsMap);
            TestHelpers.search(connector,ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(Name.NAME, TEST_USER)), handler, options);
            for (ConnectorObject user : handler) {
                System.out.println(user);
                if (TEST_USER_UID.equals(user.getUid()))
                    found = true;
                count++;
            }
            Assert.assertTrue(found);
            Assert.assertTrue(count==1);
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
            optionsMap.put(OperationOptions.OP_ATTRIBUTES_TO_GET, new String[] {Name.NAME, ATTR_CL_MEMBERS, ATTR_CL_SUPGROUP, ATTR_CL_OWNER, ATTR_CL_DATA, PredefinedAttributes.GROUPS_NAME });
            OperationOptions options = new OperationOptions(optionsMap);
            TestHelpers.search(connector,ObjectClass.GROUP, new EqualsFilter(AttributeBuilder.build(Name.NAME, "SYS1")), handler, options);
            for (ConnectorObject group : handler) {
                displayConnectorObject(group);
                if (new Uid("racfid=SYS1,profileType=group,"+SUFFIX).equals(group.getUid()))
                    found = true;
                count++;
            }
            Assert.assertTrue(found);
            Assert.assertTrue(count==1);
        } finally {
            connector.dispose();
        }
    }

    @Test//@Ignore
    public void testModifyUser() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            displayConnectorObject(getUser("IDM01", connector));
//            displayUser(getUser("CICSUSER", connector));
            // Delete the user
            deleteUser(TEST_USER_UID, connector);
    
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
            connector.create(ObjectClass.ACCOUNT, attrs, null);
            ConnectorObject user = getUser(TEST_USER, connector);
            Set<Attribute> changed = new HashSet<Attribute>();
            //
            changed.add(AttributeBuilder.build(getInstallationDataAttributeName(), "modified data"));
            changed.add(user.getUid());
            changed.add(user.getName());
            connector.update(ObjectClass.ACCOUNT, changed, null);
    
            ConnectorObject changedUser = getUser(TEST_USER, connector);
            //Attribute racfInstallationData = changedUser.getAttributeByName("racfinstallationdata");
            Attribute racfInstallationData = changedUser.getAttributeByName(getInstallationDataAttributeName());
            displayConnectorObject(changedUser);
            Assert.assertTrue(AttributeUtil.getStringValue(racfInstallationData).trim().equalsIgnoreCase("modified data"));
            displayConnectorObject(getUser("IDM01", connector));
            displayConnectorObject(getUser("IDM01", connector));
        } finally {
            connector.dispose();
        }
    }
    
    private String makeLine(String string, int length) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(string);
        while (buffer.length()<length)
            buffer.append(" ");
        return buffer.toString()+"\n";
    }
    
    @Test
    public void testCicsParser() {
        String cicsSegment =
            makeLine(" OPCLASS= 024       023       022       021       020       019       018", 80) +
            makeLine("          017       016       015       014       013       012       011", 80) +
            makeLine("          010       009       008       007       006       005       004", 80) +
            makeLine("          003       002       001", 80) +
            makeLine(" OPIDENT=", 80) +
            makeLine(" OPPRTY= 00255", 80) +
            makeLine(" TIMEOUT= 00:00 (HH:MM)", 80) +
            makeLine(" XRFSOFF= NOFORCE", 80);
        
        try {
            String cicsParser = loadParserFromFile(CICS_PARSER);
            MapTransform transform = (MapTransform)Transform.newTransform(cicsParser);
            Map<String, Object> results = (Map<String, Object>)transform.transform(cicsSegment);
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                System.out.println(entry.getKey()+"="+entry.getValue());
            }
        } catch (IOException e) {
            Assert.fail(e.toString());
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }
    
    @Test
    public void testTsoParser() {
        String tsoSegment =
            makeLine(" ACCTNUM= ACCT#", 80) +
            makeLine(" HOLDCLASS= X", 80) +
            makeLine(" JOBCLASS= A", 80) +
            makeLine(" MSGCLASS= X", 80) +
            makeLine(" PROC= ISPFPROC", 80) +
            makeLine(" SIZE= 00006133", 80) +
            makeLine(" MAXSIZE= 00000000", 80) +
            makeLine(" SYSOUTCLASS= X", 80) +
            makeLine(" USERDATA= 0000", 80) +
            makeLine(" COMMAND= ISPF PANEL(ISR@390)", 80);
        
        try {
            String tsoParser = loadParserFromFile(TSO_PARSER);
            MapTransform transform = (MapTransform)Transform.newTransform(tsoParser);
            Map<String, Object> results = (Map<String, Object>)transform.transform(tsoSegment);
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                System.out.println(entry.getKey()+"="+entry.getValue());
            }
        } catch (IOException e) {
            Assert.fail(e.toString());
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }
    
    @Test
    public void testGroupRacfParser() {
        String racfSegment =
            makeLine(" INFORMATION FOR GROUP DFPADMN", 80) +
            makeLine("     SUPERIOR GROUP=SYSADMN      OWNER=SYSADMN   CREATED=06.123 ", 80) +
            makeLine("     NO INSTALLATION DATA", 80) +
            makeLine("     NO MODEL DATA SET", 80) +
            makeLine("     TERMUACC", 80) +
            makeLine("     SUBGROUP(S)= DFPGRP1, DFPGRP2", 80) +
            makeLine("     USER(S)=      ACCESS=      ACCESS COUNT=     UNIVERSAL ACCESS=", 80) +
            makeLine("       IBMUSER         JOIN          000000              ALTER", 80) +
            makeLine("          CONNECT   ATTRIBUTES=NONE", 80) +
            makeLine("          REVOKE DATE=NONE                 RESUME DATE=NONE", 80) +
            makeLine("       DSMITH          JOIN          000002              READ", 80) +
            makeLine("          CONNECT    ATTRIBUTES=NONE", 80) +
            makeLine("          REVOKE DATE=NONE                  RESUME DATE=NONE", 80) +
            makeLine("       HOTROD          CONNECT       000004              READ", 80) +
            makeLine("          CONNECT    ATTRIBUTES=ADSP SPECIAL OPERATIONS", 80) +
            makeLine("          REVOKE DATE=NONE                  RESUME DATE=NONE", 80) +
            makeLine("       ESHAW           USE           000000              READ", 80) +
            makeLine("          CONNECT    ATTRIBUTES=NONE", 80) +
            makeLine("          REVOKE DATE=NONE                  RESUME DATE=NONE", 80) +
            makeLine("       PROJECTB        USE           000000              READ", 80) +
            makeLine("          CONNECT    ATTRIBUTES=NONE", 80) +
            makeLine("          REVOKE DATE=NONE                  RESUME DATE=NONE", 80) +
            makeLine("       ADM1            JOIN          000000              READ", 80) +
            makeLine("          CONNECT    ATTRIBUTES=OPERATIONS", 80) +
            makeLine("          REVOKE DATE=NONE                  RESUME DATE=NONE", 80) +
            makeLine("       AEHALL          USE           000000              READ", 80) +
            makeLine("          CONNECT    ATTRIBUTES=REVOKED", 80) +
            makeLine("          REVOKE DATE=NONE                  RESUME DATE=NONE", 80) +
            makeLine("  DFP INFORMATION", 80) +
            makeLine("     MGMTCLAS= DFP2MGMT", 80) +
            makeLine("     STORCLAS= DFP2STOR", 80) +
            makeLine("     DATACLAS= DFP2DATA", 80) +
            makeLine("     DATAAPPL= DFP2APPL", 80);
            
        try {
            String tsoParser = loadParserFromFile(GROUP_RACF_PARSER);
            MapTransform transform = (MapTransform)Transform.newTransform(tsoParser);
            Map<String, Object> results = (Map<String, Object>)transform.transform(racfSegment);
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                System.out.println(entry.getKey()+"="+entry.getValue());
            }
        } catch (IOException e) {
            Assert.fail(e.toString());
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }
    
    @Test
    public void testNetviewParser() {
        String netviewSegment =
            makeLine(" IC= START", 80) +
            makeLine(" CONSNAME= DJONES1", 80) +
            makeLine(" CTL= GLOBAL", 80) +
            makeLine(" MSGRECVR= YES", 80) +
            makeLine(" OPCLASS= 1,2", 80) +
            makeLine(" DOMAINS= D1,D2", 80) +
            makeLine(" MAXSIZE= 00000000", 80) +
            makeLine(" NGMFADMN= YES", 80) +
            makeLine(" NGMFVSPN= VNNN", 80);
        
        try {
            String netviewParser = loadParserFromFile(NETVIEW_PARSER);
            MapTransform transform = (MapTransform)Transform.newTransform(netviewParser);
            Map<String, Object> results = (Map<String, Object>)transform.transform(netviewSegment);
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                System.out.println(entry.getKey()+"="+entry.getValue());
            }
        } catch (IOException e) {
            Assert.fail(e.toString());
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    private String loadParserFromFile(String fileName) throws IOException {
        BufferedReader is = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(fileName)));
        StringBuffer tsoParser = new StringBuffer();
        String line = null;
        while ((line=is.readLine())!=null) {
            tsoParser.append(line+"\n");
        }
        return tsoParser.toString();
    }

    private void displayConnectorObject(ConnectorObject user) {
        Set<Attribute> attributes = user.getAttributes();
        for (Attribute attribute : attributes) {
            System.out.println(attribute.getName());
            List<Object> values = attribute.getValue();
            for (Object value : values) {
                if (value==null)
                    System.out.println("    <null>");
                else
                    System.out.println("    "+value.getClass().getName()+":"+value);
            }
        }
    }

    private ConnectorObject getUser(String accountId, RacfConnector connector) throws Exception  {
        TestHandler handler = new TestHandler();
        //TestHelpers.search(connector,ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build("racfid", accountId.getUidValue())), handler, null);
        TestHelpers.search(connector,ObjectClass.ACCOUNT, new EqualsFilter(AttributeBuilder.build(Name.NAME, accountId)), handler, null);
        for (ConnectorObject user : handler) {
            if (accountId.equals(user.getName().getNameValue()))
                return user;
        }
        return null;
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

    private void deleteUser(final Uid testUser, RacfConnector connector) {
        try {
            connector.delete(ObjectClass.ACCOUNT, testUser, null);
        } catch (UnknownUidException rte) {
            // Ignore
        }
    }

    private void deleteGroup(final Uid group, RacfConnector connector) {
        try {
            connector.delete(ObjectClass.GROUP, group, null);
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
            
            Map<String, Object> optionsMap = new HashMap<String, Object>();;
            OperationOptions options = new OperationOptions(optionsMap); 
            {
                Set<Attribute> groupAttrs = new HashSet<Attribute>();
                groupAttrs.add(new Name(TEST_GROUP1));
                Uid groupUid = connector.create(ObjectClass.GROUP, groupAttrs, options);
                Assert.assertNotNull(groupUid);
            }
            {
                Set<Attribute> groupAttrs = new HashSet<Attribute>();
                groupAttrs.add(new Name(TEST_GROUP2));
                Uid groupUid = connector.create(ObjectClass.GROUP, groupAttrs, options);
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
                List<String> allGroups = new LinkedList<String>();
                allGroups.add(TEST_GROUP1_UID.getUidValue());
                allGroups.add(TEST_GROUP2_UID.getUidValue());
                groups.add(TEST_GROUP2_UID.getUidValue());
                
                // Groups should not include the default group
                //
                attrs.add(AttributeBuilder.build(PredefinedAttributes.GROUPS_NAME, groups));
                Uid userUid = connector.create(ObjectClass.ACCOUNT, attrs, options);
                
                ConnectorObject user = getUser(TEST_USER2, connector);
                Attribute groupsAttr = user.getAttributeByName(PredefinedAttributes.GROUPS_NAME);
                Assert.assertNotNull(groupsAttr);
                List<Object> retrievedGroups = groupsAttr.getValue();
                Assert.assertTrue(retrievedGroups.size()==2);
                Assert.assertTrue(allGroups.contains(((String)retrievedGroups.get(0))));
                Assert.assertTrue(allGroups.contains(((String)retrievedGroups.get(1))));
    
                Attribute defaultGroupAttr = user.getAttributeByName(getDefaultGroupName());
                Assert.assertNotNull(groupsAttr);
                List<Object> defaultGroupAttrValue = defaultGroupAttr.getValue();
                Assert.assertEquals(defaultGroupAttrValue.get(0), TEST_GROUP1_UID.getUidValue());
            }
            
            deleteUser(TEST_USER_UID2, connector);
            deleteGroup(TEST_GROUP1_UID, connector);
            deleteGroup(TEST_GROUP2_UID, connector);
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
        } finally {
            connector.dispose();
        }
    }

    private Set<Attribute> fillInSampleUser(final String testUser) {
        Set<Attribute> attrs = new HashSet<Attribute>();
        attrs.add(new Name(testUser));
        attrs.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, new GuardedString("password".toCharArray())));
        //attrs.add(AttributeBuilder.build("objectclass", "racfUser"));
        attrs.add(AttributeBuilder.build(getInstallationDataAttributeName(), "original data"));
        return attrs;
    }

    private RacfConnector createConnector(RacfConfiguration config)
    throws Exception {
        RacfConnector connector = new RacfConnector();
        connector.init(config);
        return connector;
    }

    private RacfConfiguration createConfiguration() throws IOException {
        RacfConfiguration config = new RacfConfiguration();
        config.setHostNameOrIpAddr(HOST_NAME);
        initializeLdapConfiguration(config);
        initializeCommandLineConfiguration(config);
        config.setUseSsl(USE_SSL);
        config.setSuffix(SUFFIX);

        OurConnectorMessages messages = new OurConnectorMessages();
        Map<Locale, Map<String, String>> catalogs = new HashMap<Locale, Map<String,String>>();
        Map<String, String> foo = new HashMap<String, String>();
        ResourceBundle messagesBundle = ResourceBundle.getBundle("org.identityconnectors.rw3270.Messages");
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

    private RacfConfiguration createUserConfiguration() throws IOException {
        RacfConfiguration config = createConfiguration();
        config.setPassword(new GuardedString("password".toCharArray()));
        config.setUserName(TEST_USER);
        return config;
    }
    
    private String getLoginScript() {
        String script =
            "connection.connect();\n" +
            "connection.waitFor(\"=====>\", SHORT_WAIT);\n" +
            "connection.send(\"TSO[enter]\");\n" +
            "connection.waitFor(\"ENTER USERID -\", SHORT_WAIT);\n" +
            "connection.send(USERNAME+\"[enter]\");\n" +
            "connection.waitFor(\"Password  ===>\", SHORT_WAIT);\n" +
            "connection.send(PASSWORD);\n" +
            "connection.send(\"[enter]\");\n" +
            "connection.waitFor(\" \\\\*\\\\*\\\\* \", SHORT_WAIT);\n" +
            "connection.send(\"[enter]\");\n" +
            "connection.waitFor(\"Option ===>\", SHORT_WAIT);\n" +
            "connection.send(\"[pf3]\");\n" +
            "connection.waitFor(\"READY\\\\s{74}\", SHORT_WAIT);";
        return script;
    }

    private String getLogoffScript() {
        String script = "connection.send(\"LOGOFF[enter]\");\n";
//            "connection.send(\"LOGOFF[enter]\");\n" +
//            "connection.waitFor(\"=====>\", SHORT_WAIT);\n" +
//            "connection.dispose();\n";
        return script;
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
    protected void initializeCommandLineConfiguration(RacfConfiguration config) throws IOException {
        config.setHostTelnetPortNumber(HOST_TELNET_PORT);
        config.setConnectScript(getLoginScript());
        config.setDisconnectScript(getLogoffScript());
        config.setUserName(SYSTEM_USER );
        config.setPassword(new GuardedString(SYSTEM_PASSWORD.toCharArray()));
        config.setScriptingLanguage("GROOVY");
        config.setSegmentNames(new String[] { 
                "ACCOUNT.RACF",                     "ACCOUNT.TSO",                  "ACCOUNT.NETVIEW",
                "ACCOUNT.CICS",                     "ACCOUNT.OMVS",                 "ACCOUNT.CATALOG", 
                "GROUP.RACF" });
        config.setSegmentParsers(new String[] { 
                loadParserFromFile(RACF_PARSER),    loadParserFromFile(TSO_PARSER), loadParserFromFile(NETVIEW_PARSER), 
                loadParserFromFile(CICS_PARSER),    loadParserFromFile(OMVS_PARSER), loadParserFromFile(CATALOG_PARSER), 
                loadParserFromFile(GROUP_RACF_PARSER) });
        //config.setConnectionClassName("org.identityconnectors.rw3270.wrq.WrqConnection");
        config.setConnectionClassName("org.identityconnectors.rw3270.hod.HodConnection");
        //config.setConnectionClassName("org.identityconnectors.rw3270.freehost3270.FH3270Connection");
    }
    
    protected void initializeLdapConfiguration(RacfConfiguration config) {
        config.setHostPortNumber(HOST_PORT);
        //config.setPassword(SYSTEM_PASSWORD);
        //config.setUserName(SYSTEM_USER_LDAP);
    }

    protected String getInstallationDataAttributeName() {
        return "RACF.DATA";
    }

    protected String getDefaultGroupName() {
        return "RACF.DFLTGRP";
    }
}
