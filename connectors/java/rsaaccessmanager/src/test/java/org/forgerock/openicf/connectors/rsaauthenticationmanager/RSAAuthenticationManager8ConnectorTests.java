/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openicf.connectors.rsaauthenticationmanager;

import com.rsa.admin.data.PrincipalDTO;

import com.rsa.authmgr.common.AdminResource;

import java.util.*;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.test.common.TestHelpers;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.framework.common.objects.Schema;

import org.forgerock.openicf.connectors.rsaauthenticationmanager.RSAAuthenticationManager8Configuration;
import org.forgerock.openicf.connectors.rsaauthenticationmanager.RSAAuthenticationManager8Connector;

import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.test.common.ToListResultsHandler;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Attempts to test the {@link RSAAuthenticationManager8Connector}with the framework.
 *
 * @author Alex Babeanu (ababeanu@nulli.com)
 * www.nulli.com - Identity Solution Architects
 * 
 * @version 1.1.1.0
 * @since 1.0
 */
public class RSAAuthenticationManager8ConnectorTests {

    /*
    * Test properties from config.groovy
    */
    private static final PropertyBag PROPERTIES = TestHelpers.getProperties(RSAAuthenticationManager8Connector.class);
    // Host is a public property read from public configuration file
    //private static final String NAMING_PROVIDER_URL = PROPERTIES.getStringProperty("configuration.NamingProviderUrl");
    private static final String NAMING_PROVIDER_URL = "t3s://securidadmindev.network.dev:7002";
    
    // Login and password are private properties read from private configuration file
    private static final String CMD_CLIENT_USER = "CmdClient_s2kxf5af"; //"" - PROPERTIES.getStringProperty("configuration.CmdclientUser");
    private static final GuardedString CMD_CLIENT_USER_PWD  = new GuardedString("juhhGiR08jRKZqo8tzO9s5BZ7cgooi".toCharArray()); // - PROPERTIES.getProperty("configuration.CmdClientUserPwd", GuardedString.class);
    
    private static final String INITIAL_NAMING_FACTORY = "weblogic.jndi.WLInitialContextFactory"; //PROPERTIES.getStringProperty("configuration.InitialNamingFactory"); // "weblogic.jndi.WLInitialContextFactory"
    private static final GuardedString RSA_SSL_CLIENT_ID_STORE_PWD = new GuardedString("changeit".toCharArray()); //PROPERTIES.getProperty("configuration.RsaSslClientIdStorePwd", GuardedString.class);
    private static final GuardedString RSA_SSL_CLIENT_ID_KEY_PWD = new GuardedString("AoahSSR8E2A8Jg7gC33Z6Okv78EDBd".toCharArray()); // PROPERTIES.getProperty("configuration.RsaSslClientIdKeyPwd", GuardedString.class);
    private static final String IMS_SSL_CLIENT_PROVIDER_URL = "t3s://securidadmindev.network.dev:7022";  //PROPERTIES.getStringProperty("configuration.ImsSslClientProviderUrl");
    private static final String IMS_SSL_CLIENT_IDENTITY_KEYSTORE_FILENAME = "cacerts"; // PROPERTIES.getStringProperty("configuration.ImsSslClientIdentityKeystoreFilename");
    private static final String IMS_SSL_CLIENT_IDENTITY_KEY_ALIAS = "client-identity"; // PROPERTIES.getStringProperty("configuration.ImsSslClientIdentityKeyAlias");
    private static final String IMS_SSL_CLIENT_ROOT_CA_ALIAS = "root-ca";  //PROPERTIES.getStringProperty("configuration.ImsSslClientRootCaAlias");
    private static final String IMS_SOAP_CLIENT_PROVIDER_URL = ""; // PROPERTIES.getStringProperty("configuration.ImsSoapClientProviderUrl");
    private static final String IMS_HTTP_INVOKER_CLIENT_PROVIDER_URL = "https://securidadmindev.network.dev:7002/ims-ws/httpinvoker/CommandServer"; //PROPERTIES.getStringProperty("configuration.ImsHttpinvokerClientProviderUrl");
    
    private static final String AM_USER_MGR_PRINCIPAL = "openicf";
    private static final GuardedString AM_USER_MGR_PWD = new GuardedString("oPen-c0nn3cT2".toCharArray());
    
    private static RSAAuthenticationManager8Connector testConnector = new RSAAuthenticationManager8Connector();

    //set up logging
    private static final Log LOGGER = Log.getLog(RSAAuthenticationManager8ConnectorTests.class);

    @BeforeClass
    public static void setUp() {
        
        // TODO - Comment-out those asserts of props that are NOT 
        Assert.assertNotNull(NAMING_PROVIDER_URL );
        Assert.assertNotNull(CMD_CLIENT_USER );
        Assert.assertNotNull(CMD_CLIENT_USER_PWD);
        Assert.assertNotNull(INITIAL_NAMING_FACTORY); // "weblogic.jndi.WLInitialContextFactory"
        Assert.assertNotNull(RSA_SSL_CLIENT_ID_STORE_PWD);
        Assert.assertNotNull(RSA_SSL_CLIENT_ID_KEY_PWD);
        Assert.assertNotNull(IMS_SSL_CLIENT_PROVIDER_URL);
        Assert.assertNotNull(IMS_SSL_CLIENT_IDENTITY_KEYSTORE_FILENAME);
        Assert.assertNotNull(IMS_SSL_CLIENT_IDENTITY_KEY_ALIAS);
        Assert.assertNotNull(IMS_SSL_CLIENT_ROOT_CA_ALIAS);
        // Assert.assertNotNull(IMS_SOAP_CLIENT_PROVIDER_URL);
        Assert.assertNotNull(IMS_HTTP_INVOKER_CLIENT_PROVIDER_URL);
        Assert.assertNotNull(AM_USER_MGR_PRINCIPAL);
        Assert.assertNotNull(AM_USER_MGR_PWD);        

        //
        // Set Connector config propeties
        RSAAuthenticationManager8Configuration TestConfig = new RSAAuthenticationManager8Configuration();
        TestConfig.setCmdclientUser(CMD_CLIENT_USER);
        TestConfig.setCmdClientUserPwd(CMD_CLIENT_USER_PWD);
        TestConfig.setRsaSslClientIdStorePwd(RSA_SSL_CLIENT_ID_STORE_PWD);
        TestConfig.setRsaSslClientIdKeyPwd(RSA_SSL_CLIENT_ID_KEY_PWD);
        TestConfig.setImsSslClientProviderUrl(IMS_SSL_CLIENT_PROVIDER_URL);
        TestConfig.setImsSslClientIdentityKeystoreFilename(IMS_SSL_CLIENT_IDENTITY_KEYSTORE_FILENAME);
        TestConfig.setImsSslClientIdentityKeyAlias(IMS_SSL_CLIENT_IDENTITY_KEY_ALIAS);
        TestConfig.setImsSslClientRootCaAlias(IMS_SSL_CLIENT_ROOT_CA_ALIAS);
        //TestConfig.setImsSslClientProviderUrl(IMS_SOAP_CLIENT_PROVIDER_URL);
        TestConfig.setImsHttpinvokerClientProviderUrl(IMS_HTTP_INVOKER_CLIENT_PROVIDER_URL);
        TestConfig.setNamingProviderUrl(NAMING_PROVIDER_URL);
        TestConfig.setUserMgrPrincipal(AM_USER_MGR_PRINCIPAL);
        TestConfig.setUserMgrPwd(AM_USER_MGR_PWD);
        TestConfig.setSecurityDomain(RSAAuthenticationManager8Configuration.RSA_DOMAIN);

        // Initialize the connector
        testConnector.init(TestConfig);
        
        //Map<String, ? extends Object> configData = (Map<String, ? extends Object>) PROPERTIES.getProperty("configuration",Map.class)
        //TestHelpers.fillConfiguration(
    }

    @AfterClass
    public static void tearDown() {
        //
        //clean up resources
        testConnector.dispose();
    }

    @Test (enabled=true)
    public void testRSAConnectionTest() {
        LOGGER.info("Testing config params...");
        testConnector.getConnection().test();
        LOGGER.info("Succesful connection.");
    }
        
    /* */
    @Test (enabled=true, dependsOnMethods={"testRSAConnectionTest"})
    public void createRSAOpTest() {
        LOGGER.info("Running Test 1: Create RSA User...");
        //You can use TestHelpers to do some of the boilerplate work in running a search
        //TestHelpers.search(theConnector, ObjectClass.ACCOUNT, filter, handler, null);
        
        // CreateOp Signature:
        // Uid create(final ObjectClass objectClass, final Set<Attribute> createAttributes, final OperationOptions options)]
        
        // New User's ObjectClass
        ObjectClass ObjC = new ObjectClass("__ACCOUNT__");
        
        // New User's Attributes
        HashSet<Attribute> CreateAttrs = new HashSet<Attribute> ();
        // User Pwd
        CreateAttrs.add(new Name("JoDoe3"));
        CreateAttrs.add(new Uid("JoDoe3"));
        CreateAttrs.add(AttributeBuilder.buildPassword(new GuardedString("oPen-c0nn3cT2".toCharArray())));
        //CreateAttrs.add(AttributeBuilder.build(PrincipalDTO.LOGINUID, "JoDoe3"));
        CreateAttrs.add(AttributeBuilder.build(PrincipalDTO.FIRST_NAME, "Joe3"));
        CreateAttrs.add(AttributeBuilder.build(PrincipalDTO.LAST_NAME, "Doe3"));
        CreateAttrs.add(AttributeBuilder.build(PrincipalDTO.DESCRIPTION, "IDM Consultant"));
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_EMPLOYEE_NB, "00518999"));
        CreateAttrs.add(AttributeBuilder.build(PrincipalDTO.ADMINISTRATOR_FLAG, false));
        CreateAttrs.add(AttributeBuilder.build(PrincipalDTO.EMAIL,"jodoe3@suncor.com"));
        CreateAttrs.add(AttributeBuilder.build(PrincipalDTO.MIDDLE_NAME,"Albert3"));
        // Custom Attribs
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_ALLOWED_TO_CREATE_PIN, Boolean.TRUE));
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_REQUIRED_TO_CREATE_PIN, Boolean.FALSE));
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_START_DATE, "2013/09/01"));
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_START_HOUR, "8"));
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_END_DATE, "2014/09/01"));
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_END_HOUR, "5"));
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_SECRET_WORD, "Connector"));
        //CreateAttrs.add(AttributeBuilder.build(PrincipalDTO.SECURITY_DOMAIN,RSAAuthenticationManager8Configuration.RSA_DOMAIN));
        // TODO - Security Questions ?

        // AM Account Add-on attributes:
        CreateAttrs.add(AttributeBuilder.build(AdminResource.DEFAULTSHELL, "/bin/sh"));
        // Uncomment if needed:
        /*
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.ATTR_WIN_PWD, "Pwd-1234"));
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.ATTR_SHELL_ALLOWED, true));
        */
        
        // Add Group Memberships
        ArrayList<String> AddGroups = new ArrayList<String>();
        AddGroups.add("SSLVPN");
        AddGroups.add("Remote Access");
        AddGroups.add("Certigard");
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_GROUPS, AddGroups));
        
        // Add Tokens
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_SN, "000031953940"));
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_SN, "000039688583"));
        // This one is disabled:
        CreateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_SN, "000024017653"));
        
        /* */
        // Create user
        Uid uid = testConnector.create(ObjC, CreateAttrs, new OperationOptionsBuilder().build());
        if (uid == null)
            throw new ConfigurationException ("UID is blank, user creation failure");
        else {
            System.out.println("\n New user created with UID = " + uid.getUidValue());
        }
    }
    /* */

    /* */
    @Test (enabled=true, dependsOnMethods={"createRSAOpTest"}) //createRSAOpTest  "})//, dependsOnMethods={"equalsSearchOpTest"})//, dependsOnMethods={"testRSAConnectionTest"} )
    public void updateUserRSAOpTest() {
        LOGGER.info("Running Test 2: Update RSA User...");
        //You can use TestHelpers to do some of the boilerplate work in running a search
        //TestHelpers.search(theConnector, ObjectClass.ACCOUNT, filter, handler, null);
        
        // New User's ObjectClass
        ObjectClass ObjC = new ObjectClass("__ACCOUNT__");
        
        // New User's Attributes
        // Update User
        Uid UpdUsr = new Uid ("JoDoe3");
        HashSet<Attribute> UpdateAttrs = new HashSet<Attribute> ();
        
        // Un-comment the attributes you want to modify:
        // User Pwd
        //UpdateAttrs.add(AttributeBuilder.buildPassword(new GuardedString("oPen-c0nn3cT".toCharArray())));
        //UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.LOGINUID, "JoDoe2"));
        UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.FIRST_NAME, "Joe3MOD"));
        UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.LAST_NAME, "Doe3MOD"));
        UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.DESCRIPTION, "IDM Consultant MOD"));
        //UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_EMPLOYEE_NB, "00518999"));
        //UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.ADMINISTRATOR_FLAG, Boolean.FALSE));
        UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.EMAIL,"jodoe3mod@suncor.com"));
        UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.MIDDLE_NAME,"Albert3Mod"));
        // New Expiration Date
        UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.EXPIRATION_DATE, "2014/06/22"));
        // New Start Date
        UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.START_DATE, "2013/09/03"));
        // New Expire Lockout Date
        // Date principal will no longer be locked out of the system.
        UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.EXPIRE_LOCKOUT_DATE, "2013/09/03"));
        /* */
        //  Flag indicating whether principal is locked out of the system.
        UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.LOCKOUT_FLAG, Boolean.FALSE));
        // Flag indicating whether the principal is enabled.
        UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.ENABLE_FLAG, Boolean.TRUE));
        // Flag indicating if principal can be impersonated.
        UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.IMPERSONATABLE_FLAG, Boolean.TRUE));
        //  Flag indicating if principal can impersonate others.
        UpdateAttrs.add(AttributeBuilder.build(PrincipalDTO.IMPERSONATOR_FLAG, Boolean.TRUE));
        //Another example using TestHelpers
        //List<ConnectorObject> results = TestHelpers.searchToList(theConnector, ObjectClass.GROUP, filter);
        
        // Extended/custom Attribds
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_ALLOWED_TO_CREATE_PIN, Boolean.TRUE));
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_REQUIRED_TO_CREATE_PIN, Boolean.TRUE));
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_SECRET_WORD, "WatermelonMOD"));
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_EMPLOYEE_NB, "00518999"));
        // Tepm Start Date
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_START_DATE, "2013/12/01"));
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_END_DATE, "2014/07/01"));
        // Add Group Memberships
        ArrayList<String> AddGroups = new ArrayList<String>();
        AddGroups.add("SSLVPN");
        AddGroups.add("CSG User");
        AddGroups.add("CSC Admin");
        AddGroups.add("CSC General");
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_GROUPS, AddGroups));
        
        // Token operations
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_LOST, "true"));
        //UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_UNASSIGN, "true"));
        //UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_NEW_PIN_MODE, "false"));
        //UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_CLEAR_PIN, "true"));
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_DISABLED, "true"));
        
        // Default SHell
        UpdateAttrs.add(AttributeBuilder.build(AdminResource.DEFAULTSHELL, "/bin/ksh"));
        
        // Update
        // No options for update
        OperationOptionsBuilder OpsBuilder = new OperationOptionsBuilder();
        Uid uid = testConnector.update(ObjC, UpdUsr, UpdateAttrs, OpsBuilder.build()); 
    }
   /* */
   @Test (enabled=false, dependsOnMethods={"updateUserRSAOpTest"}) //createRSAOpTest"}) //equalsSearchOpTest"})//, dependsOnMethods={"updateUserRSAOpTest"})
    public void updateTokenOp() {
        // New User's Attributes
        // Update User
        Uid UpdUsr = new Uid("JoDoe3");
        HashSet<Attribute> UpdateAttrs = new HashSet<Attribute>();
        // New User's ObjectClass
        ObjectClass ObjC = new ObjectClass("__ACCOUNT__");
        
        // Token operations
        //UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_LOST, "true"));
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_UNASSIGN, "true"));
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_NEW_PIN_MODE, "false"));
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_CLEAR_PIN, "true"));
        //UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_DISABLED, "true"));
        
        // Update
        // No options for update
        OperationOptionsBuilder OpsBuilder = new OperationOptionsBuilder();
        Uid uid = testConnector.update(ObjC, UpdUsr, UpdateAttrs, OpsBuilder.build());
    }
    
   @Test (enabled=false, dependsOnMethods={"updateTokenOp"})
   public void assignTokenOpTest() {
       LOGGER.info("Running Test 3: Assign Token to RSA User...");
       //You can use TestHelpers to do some of the boilerplate work in running a search
       //TestHelpers.search(theConnector, ObjectClass.ACCOUNT, filter, handler, null);
       
       //User ObjectClass
       ObjectClass ObjC = new ObjectClass(RSAAuthenticationManager8Configuration.TOKEN_OBJECTCLASS);
       
       // User ID
       Uid Usr = new Uid ("JoDoe3");
       // Token ASSIGN action:
       OperationOptionsBuilder OpsBuilder = new OperationOptionsBuilder();
       OpsBuilder.setOption(RSAAuthenticationManager8Configuration.TOKEN_OP_OPTION_ASSIGN_NAME, RSAAuthenticationManager8Configuration.TOKEN_OP_OPTION_ASSIGN);
       
       // Set Token SNs to assign to user:
       List<String> tokens = Arrays.asList("000024017618", "000039766597", "000100015179");
       
       HashSet<Attribute> UpdateAttrs = new HashSet<Attribute> ();
       UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN_SN_LIST, tokens));
       
       testConnector.update(ObjC, Usr, UpdateAttrs, OpsBuilder.build());
   }
   
    @Test (enabled=false, dependsOnMethods={"assignTokenOpTest"})
    public void disableTokenOpTest() {
        LOGGER.info("Running Test 4: disable Token ...");
        //You can use TestHelpers to do some of the boilerplate work in running a search
        //TestHelpers.search(theConnector, ObjectClass.ACCOUNT, filter, handler, null);
        
        //User ObjectClass
        ObjectClass ObjC = new ObjectClass("__TOKEN__");
        
        // User ID - not really necessary for this operation
        Uid Usr = new Uid ("JoDoe3");
        
        // Token DISABLE action:
        OperationOptionsBuilder OpsBuilder = new OperationOptionsBuilder();
        OpsBuilder.setOption(RSAAuthenticationManager8Configuration.TOKEN_OP_OPTION_DISABLE_NAME, RSAAuthenticationManager8Configuration.TOKEN_OP_OPTION_DISABLE);
        
        // Set Token SNs to revoke from their respective users:
        List<String> tokens = Arrays.asList("000024017618", "000039766597", "000100015179");
        
        HashSet<Attribute> UpdateAttrs = new HashSet<Attribute> ();
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN_SN_LIST, tokens));
        
        testConnector.update(ObjC, Usr, UpdateAttrs, OpsBuilder.build());
    }
    @Test (enabled=false, dependsOnMethods={"disableTokenOpTest"})
    public void enableTokenOpTest() {
        LOGGER.info("Running Test 5: disable Token ...");
        //You can use TestHelpers to do some of the boilerplate work in running a search
        //TestHelpers.search(theConnector, ObjectClass.ACCOUNT, filter, handler, null);
        
        //User ObjectClass
        ObjectClass ObjC = new ObjectClass("__TOKEN__");
        
        // User ID - not really necessary for this operation
        Uid Usr = new Uid ("JoDoe3");
        
        // Token DISABLE action:
        OperationOptionsBuilder OpsBuilder = new OperationOptionsBuilder();
        OpsBuilder.setOption(RSAAuthenticationManager8Configuration.TOKEN_OP_OPTION_ENABLE_NAME, RSAAuthenticationManager8Configuration.TOKEN_OP_OPTION_ENABLE);
        
        // Set Token SNs to revoke from their respective users:
        List<String> tokens = Arrays.asList("000024017618", "000039766597", "000100015179");

        HashSet<Attribute> UpdateAttrs = new HashSet<Attribute> ();
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN_SN_LIST, tokens));
        
        testConnector.update(ObjC, Usr, UpdateAttrs, OpsBuilder.build());
    }
   
    @Test (enabled=false, dependsOnMethods={"enableTokenOpTest"})
    public void revokeTokenOpTest() {
        LOGGER.info("Running Test 6: Revoke Token from RSA User...");
        //You can use TestHelpers to do some of the boilerplate work in running a search
        //TestHelpers.search(theConnector, ObjectClass.ACCOUNT, filter, handler, null);
        
        //User ObjectClass
        ObjectClass ObjC = new ObjectClass("__TOKEN__");
        
        // User ID - not really necessary for this operation
        Uid Usr = new Uid ("JoDoe3");
        
        // Token REVOKE action:
        OperationOptionsBuilder OpsBuilder = new OperationOptionsBuilder();
        OpsBuilder.setOption(RSAAuthenticationManager8Configuration.TOKEN_OP_OPTION_REVOKE_NAME, RSAAuthenticationManager8Configuration.TOKEN_OP_OPTION_REVOKE);
        
        // Set Token SNs to revoke from their respective users:
        List<String> tokens = Arrays.asList("000024017618", "000039766597", "000100015179");
        
        HashSet<Attribute> UpdateAttrs = new HashSet<Attribute> ();
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN_SN_LIST, tokens));
        
        testConnector.update(ObjC, Usr, UpdateAttrs, OpsBuilder.build());
    }
   
    @Test (enabled=true, dependsOnMethods={"updateUserRSAOpTest"}) //dependsOnMethods={"revokeTokenOpTest"})
    public void linkUser2GroupOpTest() {
        LOGGER.info("Running Test 7: Assign User to a group...");
        //You can use TestHelpers to do some of the boilerplate work in running a search
        //TestHelpers.search(theConnector, ObjectClass.ACCOUNT, filter, handler, null);
        
        //User ObjectClass
        ObjectClass ObjC = new ObjectClass("__GROUP__");
        
        // User ID
        Uid Usr = new Uid ("JoDoe3");
        // Token ASSIGN action:
        OperationOptionsBuilder OpsBuilder = new OperationOptionsBuilder();
        OpsBuilder.setOption(RSAAuthenticationManager8Configuration.GROUP_OP_OPTION_LINK_NAME, RSAAuthenticationManager8Configuration.GROUP_OP_OPTION_LINK);
        
        // GroupName:
        HashSet<Attribute> UpdateAttrs = new HashSet<Attribute> ();
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_GROUP_NAME, "CSC Admin"));
        
        testConnector.update(ObjC, Usr, UpdateAttrs, OpsBuilder.build());
    }
    
    @Test (enabled=true, dependsOnMethods={"linkUser2GroupOpTest"})
    public void unlinkUserFromGroupOpTest() {
        LOGGER.info("Running Test 8: Unlink User from a group...");
        
        //User ObjectClass
        ObjectClass ObjC = new ObjectClass("__GROUP__");
        
        // User ID
        Uid Usr = new Uid ("JoDoe3");
        // Token ASSIGN action:
        OperationOptionsBuilder OpsBuilder = new OperationOptionsBuilder();
        OpsBuilder.setOption(RSAAuthenticationManager8Configuration.GROUP_OP_OPTION_UNLINK_NAME, RSAAuthenticationManager8Configuration.GROUP_OP_OPTION_UNLINK);
        
        // GroupName:
        HashSet<Attribute> UpdateAttrs = new HashSet<Attribute> ();
        UpdateAttrs.add(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_GROUP_NAME, "CSC Admin"));
        
        testConnector.update(ObjC, Usr, UpdateAttrs, OpsBuilder.build());
    }
   
    @Test (enabled=true, dependsOnMethods={"unlinkUserFromGroupOpTest"})//{"unlinkUserFromGroupOpTest"})
    public void containsSearchOpTest() {
        LOGGER.info("Running Test 9: Contains Search...");

        // Results Handler
        ToListResultsHandler handler = new ToListResultsHandler();
        
        // Query Options: set Search Limit
        OperationOptionsBuilder OpsBuilder = new OperationOptionsBuilder();
        OpsBuilder.setOption(RSAAuthenticationManager8Configuration.SEARCH_LIMIT_NAME, RSAAuthenticationManager8Configuration.SEARCH_LIMIT_DEFAULT);
        
        // Search Filter: LoginID contains "JoDoe"
        ContainsFilter contains = new ContainsFilter (AttributeBuilder.build(Name.NAME, "JoDoe"));
        
        // Run Search
        TestHelpers.search(testConnector, ObjectClass.ACCOUNT, contains, handler, OpsBuilder.build());
        
        // Assert results
        Assert.assertTrue(handler.getObjects().size() > 0, "zero results returned");
        
        // Read results
        int nbRes = handler.getObjects().size();
        System.out.println("handled " + Integer.toString(nbRes) + " results.");
        // Loop through results
        for (ConnectorObject result : handler.getObjects()) {
            Set<Attribute> attribs = result.getAttributes();
            Iterator<Attribute> attIt = attribs.iterator();
            // Loop through attributes
            while (attIt.hasNext()) {
                Attribute att = attIt.next();
                System.out.println ("\n Attribute: " + att.getName() + " - value: " + att.getValue().toString());
            }
            System.out.println ("\n ------------------------------");
        }
    }
    
    @Test (enabled=true, dependsOnMethods={"containsSearchOpTest"}) //testRSAConnectionTest"}) //updateTokenOp"}) //containsSearchOpTest"})
    public void equalsSearchOpTest() {
        LOGGER.info("Running Test 10: Equals Search...");

        // Results Handler
        ToListResultsHandler handler = new ToListResultsHandler();
        
        // Query Options: set Search Limit
        OperationOptionsBuilder OpsBuilder = new OperationOptionsBuilder();
        OpsBuilder.setOption(RSAAuthenticationManager8Configuration.SEARCH_LIMIT_NAME, RSAAuthenticationManager8Configuration.SEARCH_LIMIT_DEFAULT);
        
        // Search Filter: LoginID contains "JoDoe"
        EqualsFilter contains = new EqualsFilter (AttributeBuilder.build(Name.NAME, "JoDoe3"));
        
        // Run Search
        TestHelpers.search(testConnector, ObjectClass.ACCOUNT, contains, handler, OpsBuilder.build());
        
        // Assert results
        Assert.assertTrue(handler.getObjects().size() > 0, "zero results returned");
        
        // Read results
        int nbRes = handler.getObjects().size();
        System.out.println("handled " + Integer.toString(nbRes) + " results.");
        // Loop through results
        for (ConnectorObject result : handler.getObjects()) {
            Set<Attribute> attribs = result.getAttributes();
            Iterator<Attribute> attIt = attribs.iterator();
            // Loop through attributes
            while (attIt.hasNext()) {
                Attribute att = attIt.next();
                if (att != null)
                    if (att.getValue() != null)
                        System.out.println ("\n Attribute: " + att.getName() + " - value: " + att.getValue().toString());
                    else
                        System.out.println ("\n Attribute: " + att.getName() + " - value: NULL");
            }
            System.out.println ("\n ------------------------------");
        }
    }
    
    @Test (enabled=false, dependsOnMethods={"equalsSearchOpTest"}) //{"equalsSearchOpTest"})
    public void searchAllOpTest() {
        LOGGER.info("Running Test 10: Search All...");

        // Results Handler
        ToListResultsHandler handler = new ToListResultsHandler();
        
        // Query Options: set Search Limit
        OperationOptionsBuilder OpsBuilder = new OperationOptionsBuilder();
        //OpsBuilder.setOption(RSAAuthenticationManager8Configuration.SEARCH_LIMIT_NAME, RSAAuthenticationManager8Configuration.SEARCH_LIMIT_DEFAULT);
        
        // Search Filter: LoginID contains "JoDoe"
        EqualsFilter contains = new EqualsFilter (AttributeBuilder.build(Name.NAME, ""));
        
        // Run Search
        TestHelpers.search(testConnector, ObjectClass.ACCOUNT, contains, handler, OpsBuilder.build());
        
        // Assert results
        Assert.assertTrue(handler.getObjects().size() > 0, "zero results returned");
        
        // Read results
        int nbRes = handler.getObjects().size();
        System.out.println("handled " + Integer.toString(nbRes) + " results.");
        // Loop through results
        /*
        for (ConnectorObject result : handler.getObjects()) {
            Set<Attribute> attribs = result.getAttributes();
            Iterator<Attribute> attIt = attribs.iterator();
            // Loop through attributes
            while (attIt.hasNext()) {
                Attribute att = attIt.next();
                if (att != null)
                    if (att.getValue() != null)
                        System.out.println ("\n Attribute: " + att.getName() + " - value: " + att.getValue().toString());
                    else
                        System.out.println ("\n Attribute: " + att.getName() + " - value: NULL");
            }
            System.out.println ("\n ------------------------------");
        }
        */
    }
    
    @Test (enabled=true, dependsOnMethods={"equalsSearchOpTest"})//, searchAllOpTest dependsOnMethods={""})
    public void connAliveTest() {
        LOGGER.info ("Checking if the connection is still alive...");
        testConnector.checkAlive();
        LOGGER.info ("The connection is still alive.");
    }
    
   @Test (enabled=true, dependsOnMethods={"equalsSearchOpTest"}) //connAliveTest"})//, dependsOnMethods={"equalsSearchOpTest"})
   public void deleteUserRSAOpTest() {
       LOGGER.info("Running LAST Upd test: Delete RSA User...");
       //You can use TestHelpers to do some of the boilerplate work in running a search
       //TestHelpers.search(theConnector, ObjectClass.ACCOUNT, filter, handler, null);
       
       //User ObjectClass
       ObjectClass ObjC = new ObjectClass("__ACCOUNT__");
       
       // User ID
       Uid DelUsr = new Uid ("JoDoe3");
       // No options options for delete
       OperationOptionsBuilder OpsBuilder = new OperationOptionsBuilder();
       testConnector.delete(ObjC, DelUsr, OpsBuilder.build());
   }
   
    @Test (enabled=true, dependsOnMethods={"deleteUserRSAOpTest"}) //deleteUserRSAOpTest
    public void schemaOpTest() {
        LOGGER.info("Running Schema Test: fetch supported schema...");
        //You can use TestHelpers to do some of the boilerplate work in running a search
        //TestHelpers.search(theConnector, ObjectClass.ACCOUNT, filter, handler, null);
        
        Schema sch = testConnector.schema();
        Set <ObjectClassInfo> objs = sch.getObjectClassInfo();
        System.out.println ("------ Suported ObjClasses:");
        for (ObjectClassInfo info: objs) {
            System.out.println(info.getType());
            Set<AttributeInfo> attrsInfo = info.getAttributeInfo();
            for (AttributeInfo attrInfo : attrsInfo) {
                System.out.println(" --- " + attrInfo.getName());
            }
        }
    }
    
    protected ConnectorFacade getFacade(RSAAuthenticationManager8Configuration config) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        // **test only**
        APIConfiguration impl = TestHelpers.createTestConfiguration(RSAAuthenticationManager8Connector.class, config);
        return factory.newInstance(impl);
    }
}
