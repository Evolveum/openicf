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

import com.rsa.admin.AddPrincipalsCommand;
import com.rsa.admin.DeletePrincipalsCommand;
import com.rsa.admin.EndSearchPrincipalsIterativeCommand;
import com.rsa.admin.GetPrincipalGroupsCommand;
import com.rsa.admin.LinkGroupPrincipalsCommand;
import com.rsa.admin.SearchGroupsCommand;
import com.rsa.admin.SearchPrincipalsCommand;
import com.rsa.admin.SearchPrincipalsIterativeCommand;
import com.rsa.admin.SearchSecurityDomainCommand;
import com.rsa.admin.UnlinkGroupPrincipalsCommand;
import com.rsa.admin.UpdatePrincipalCommand;
import com.rsa.admin.data.AttributeDTO;
import com.rsa.admin.data.GroupDTO;
import com.rsa.admin.data.ModificationDTO;
import com.rsa.admin.data.PrincipalDTO;
import com.rsa.admin.data.SecurityDomainDTO;
import com.rsa.admin.data.UpdatePrincipalDTO;
import com.rsa.authmgr.admin.agentmgt.SearchAgentsCommand;
import com.rsa.authmgr.admin.agentmgt.data.AgentConstants;
import com.rsa.authmgr.admin.agentmgt.data.ListAgentDTO;
import com.rsa.authmgr.admin.principalmgt.AddAMPrincipalCommand;
import com.rsa.authmgr.admin.principalmgt.LookupAMPrincipalCommand;
import com.rsa.authmgr.admin.principalmgt.UpdateAMPrincipalCommand;
import com.rsa.authmgr.admin.principalmgt.data.AMPrincipalDTO;
import com.rsa.authmgr.admin.tokenmgt.EnableTokensCommand;
import com.rsa.authmgr.admin.tokenmgt.LinkTokensWithPrincipalCommand;
import com.rsa.authmgr.admin.tokenmgt.ListTokensByPrincipalCommand;
import com.rsa.authmgr.admin.tokenmgt.LookupTokenCommand;
import com.rsa.authmgr.admin.tokenmgt.LookupTokenEmergencyAccessCommand;
import com.rsa.authmgr.admin.tokenmgt.ResynchronizeTokenCommand;
import com.rsa.authmgr.admin.tokenmgt.UnlinkTokensFromPrincipalsCommand;
import com.rsa.authmgr.admin.tokenmgt.UpdateTokenCommand;
import com.rsa.authmgr.admin.tokenmgt.UpdateTokenEmergencyAccessCommand;
import com.rsa.authmgr.admin.tokenmgt.data.ListTokenDTO;
import com.rsa.authmgr.admin.tokenmgt.data.TokenDTO;
import com.rsa.authmgr.admin.tokenmgt.data.TokenEmergencyAccessDTO;
import com.rsa.authmgr.common.AdminResource;
import com.rsa.command.CommandException;
import com.rsa.command.exception.ConcurrentUpdateException;
import com.rsa.command.exception.DataNotFoundException;
import com.rsa.command.exception.DuplicateDataException;
import com.rsa.command.exception.InsufficientPrivilegeException;
import com.rsa.command.exception.InvalidArgumentException;
import com.rsa.command.exception.ObjectInUseException;
import com.rsa.common.search.Filter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.spi.PoolableConnector;

/**
 * Main implementation of the RSA AM 8 Connector Connector.
 * <p>
 * The RSA Authentication Manager 8 connector has the following setup
 * prerequisites:
 * <br/>
 * <ul>
 * <li> The following JAR dependencies must be present in classpath:
 * <ul>
 * <li>ws-constants-1.0.jar</li>
 * <li>commons-io-2.2-1.0.jar</li>
 * <li>ognl-1.0.jar</li>
 * <li>commons-logging-1.1.1-1.0.jar</li>
 * <li>commons-httpclient-1.0.jar</li>
 * <li>commons-logging-1.0.jar</li>
 * <li>log4j-1.2.12rsa-1-1.0.jar</li>
 * <li>axis-saaj-1.0.jar</li>
 * <li>connector-framework-1.1.0.0-SNAPSHOT-javadoc-1.0.jar</li>
 * <li>am-client-1.0.jar</li>
 * <li>axis-jaxrpc-1.0.jar</li>
 * <li>iScreen-1-1-0rsa-2-1.0.jar</li>
 * <li>iScreen-ognl-1-1-0rsa-2-1.0.jar</li>
 * <li>aopalliance-1.0.jar</li>
 * <li>xercesImpl-1.0.jar</li>
 * <li>dependencies.txt
 * <li>spring-beans-1.0.jar</li>
 * <li>spring-aop-1.0.jar</li>
 * <li>connector-framework-internal-1.1.0.0-SNAPSHOT-1.0.jar</li>
 * <li>spring-context-1.0.jar</li></li>
 * <li>iScreen-ognl-1.0.jar</li>
 * <li>spring-expression-1.0.jar</li>
 * <li>jdom-1.0-1.0.jar</li>
 * <li>commons-beanutils-1.0.jar</li>
 * <li>commons-lang-1.0.jar</li>
 * <li>clu-common-1.0.jar</li>
 * <li>commons-discovery-0.2-1.0.jar</li>
 * <li>wlfullclient-1.0.jar</li>
 * <li>spring-asm-1.0.jar</li>
 * <li>connector-framework-1.1.0.0-SNAPSHOT-1.0.jar</li>
 * <li>axis-1.0.jar</li>
 * <li>commons-codec-1.0.jar</li>
 * <li>log4j-1.0.jar</li>
 * <li>iScreen-1.0.jar</li>
 * <li>ognl-2.6.7-1.0.jar</li>
 * <li>commons-beanutils-1.8.3-1.0.jar</li>
 * <li>commons-discovery-1.0.jar</li>
 * <li>spring-web-1.0.jar</li>
 * <li>spring-context-support-1.0.jar</li>
 * <li>wsdl4j-1.0.jar</li>
 * <li>test-contract-1.3-1.0.jar</li>
 * <li>testng-5.7-jdk15-1.0.jar</li>
 * <li>commons-lang-2.2-1.0.jar</li>
 * <li>ims-systemfields-4.3.0.0.0-1.0.jar</li>
 * <li>axis-1.3-1.0.jar</li>
 * <li>cryptoj-5.0-1.0.jar</li>
 * <li>connector-framework-1.1.0.0-SNAPSHOT-tests-1.0.jar</li>
 * <li>ws-extras-1.0.jar</li>
 * <li>spring-core-1.0.jar</li>
 * </ul>
 * </li>
 * <br/>
 * <li>The root certificate of the RSA server must be added to the local keystore (using keytool).
 * The local keystore may reside in JAVA_HOME/jre/lib/security for example.</li>
 * <br/>
 * <li>The RSA Authentication Manager 8 connector uses the RSA COmmand Client server, which
 * requires credentials. These command client credentials can be retrieved from the RSA
 * server by running "rsautil manage-secrets --action list" for the server's
 * RSA_HOME/utils directory.</li>
 * <br/>
 * <li>The connector requires a RSA Authentication manager 8 service account to perform
 * it operation. This special account must have User Manager role in RSA. The relevant
 * credentials can be set using the 'UserMgrPrincipal' and 'UserMgrPwd' Connector
 * configuration properties. The 'UserMgrPrincipal' defaults to "openicf".</li>
 * </ul>
 * </p>
 * 
 * @author Alex Babeanu (ababeanu@nulli.com)
 *  www.nulli.com - Identity Solution Architects
 * 
 * @version 1.1
 * @since 1.0
 */
@ConnectorClass(
        displayNameKey = "RSAAuthenticationManager8.connector.display",
        configurationClass = RSAAuthenticationManager8Configuration.class)
public class RSAAuthenticationManager8Connector implements
        PoolableConnector
        , CreateOp
        , DeleteOp
        , SearchOp<Filter>
        , TestOp
        , UpdateOp
        , SchemaOp
 {
    /**
     * Setup logging for the {@link RSAAuthenticationManager8Connector}.
     */
    
    private static final Log logger = Log.getLog(RSAAuthenticationManager8Connector.class);

    /**
     * Place holder for the Connection created in the init method.
     */
    private RSAAuthenticationManager8Connection connection;

    /**
     * Gets the Connection context for this connector.
     *
     * @return The current RSA Connection
     */
    public RSAAuthenticationManager8Connection getConnection() {
        return connection;
    }

    /**
     * Place holder for the {@link Configuration}passed into the init() method
     * {@link org.forgerock.openicf.connectors.rsaauthenticationmanager.RSAAuthenticationManager8Connector#init(org.identityconnectors.framework.spi.Configuration)}.
     */
    private RSAAuthenticationManager8Configuration configuration;

    /**
     * Gets the Configuration context for this connector.
     *
     * @return The current {@link Configuration}
     */
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * The RSA User Principal this connector instance modifies.
     */
    // private PrincipalDTO UserPrincipal;

    /**
     * Flag used internally to identify full user searches.
     */
    private Boolean searchAll = false;
    
    /**
     * Callback method to receive the {@link Configuration}.
     *
     * @param configuration the new {@link Configuration}
     * @see org.identityconnectors.framework.spi.Connector#init(org.identityconnectors.framework.spi.Configuration)
     */
    public void init(final Configuration configuration) {
        this.configuration = (RSAAuthenticationManager8Configuration) configuration;
        this.connection = new RSAAuthenticationManager8Connection(this.configuration);
    }

    /**
     * Disposes of the {@link RSAAuthenticationManager8Connector}'s resources.
     *
     * @see org.identityconnectors.framework.spi.Connector#dispose()
     */
    public void dispose() {
        //configuration = null;
        if (connection != null) {
            connection.dispose();
            connection = null;
        }
        configuration = null;
    }


    /******************
     * SPI Operations
     ******************/

    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objectClass, final Set<Attribute> createAttributes, final OperationOptions options) {
        
        String Uid = null;
        String LoginID = null;
        
        logger.info("Creating object of ObjectClass:" + objectClass.getDisplayNameKey());
        logger.info("Creation contains {0} attributes", createAttributes.size());
        
        // Extract attributes and create User and Account in IMS and AM
        if (objectClass.is("__ACCOUNT__")) {
            
            logger.info("Creating User Account...");

            // Read attributes from input
            logger.info("Reading Creation attributes...");
            AttributesAccessor aa = new AttributesAccessor(createAttributes);

            // Operational Attribs
            GuardedString Password = aa.getPassword();
            if (Password == null) {
                       throw new IllegalArgumentException("The Password attribute cannot be null.");
            }
            Boolean Enabled = aa.getEnabled(true);
            
            // Getting UserID
            //String UserId = aa.findString(PrincipalDTO.LOGINUID);
            Uid CreateUid = aa.getUid();
            String UserId;
            if (CreateUid == null)
                // If no UID attribute provided, use the Name
                UserId = aa.getName().getNameValue();
            else {
                UserId = aa.getUid().getUidValue();
                if ((UserId == null) || (UserId.isEmpty()))
                    UserId = aa.getName().getNameValue();
            }
            
            // Other attribs
            String First = aa.findString(PrincipalDTO.FIRST_NAME);
            String Last = aa.findString(PrincipalDTO.LAST_NAME);
            String Descr = aa.findString(PrincipalDTO.DESCRIPTION);
            String MiddleName = aa.findString(PrincipalDTO.MIDDLE_NAME);
            String EmpNb = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_EMPLOYEE_NB);
            String Email = aa.findString(PrincipalDTO.EMAIL);
            Boolean AdminRole = aa.findBoolean(PrincipalDTO.ADMINISTRATOR_FLAG);
            if (AdminRole == null)
                AdminRole = Boolean.FALSE;
            
            // AM extension attribs
            String DefShell = aa.findString(AdminResource.DEFAULTSHELL);
            Boolean ShellAllowed = Boolean.FALSE;
            
            if (DefShell != null) {
                // Default shell value provided, need an AM account.
                ShellAllowed = aa.findBoolean(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_SHELL_ALLOWED);
                if (ShellAllowed == null)
                    // No Default Shell Allowed value provided, assume TRUE since a Shell value is provided.
                    ShellAllowed = Boolean.TRUE;
                // Else use whatever Shell Allowed value is provided
            }
        
            // Uncomment to enable
            /*
            String WinPwd = aa.findString(configuration.ATTR_WIN_PWD);
            */
            
            // Uncomment if the Security Domain needs to be passed as parameter to create
            // Currently determined at Connection time
            // String SDGuid = aa.findString(PrincipalDTO.SECURITY_DOMAIN);
            
            // Create is a 2-step process: 1) create the IMS user and 2) create the AM Account
            // 1- Create IMS User
            
            PrincipalDTO principal = new PrincipalDTO();
            principal.setUserID(UserId);
            principal.setFirstName(First);
            principal.setLastName(Last);
            principal.setPassword(RSAAuthenticationManager8Utils.getPlainPassword(Password));

            principal.setEnabled(Enabled);
            principal.setCanBeImpersonated(false);
            principal.setTrustToImpersonate(false);

            principal.setSecurityDomainGuid(this.connection.getDomain().getGuid());
            principal.setIdentitySourceGuid(this.connection.getIdSource().getGuid());
            // require user to change password at next login
            principal.setPasswordExpired(true);
            principal.setDescription(Descr);
            principal.setMiddleName(MiddleName);
            principal.setEmail(Email);
            principal.setAdminRole(AdminRole);
            
            // Add custom Attributes
            ArrayList<AttributeDTO> attrs = new ArrayList<AttributeDTO> ();
            
            // DATA Attribs
            // Employee Number
            AttributeDTO attr = new AttributeDTO();
            attr.setName(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_EMPLOYEE_NB);
            String[] EmpNbs = {EmpNb};
            attr.setValues(EmpNbs);
            attrs.add(attr);
            
            // Allowed to create pin
            attr = new AttributeDTO();
            attr.setName(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_ALLOWED_TO_CREATE_PIN);
            Boolean[] AllowPin = {aa.findBoolean(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_ALLOWED_TO_CREATE_PIN)};
            attr.setValues(AllowPin);
            attrs.add(attr);
            
            // Required to create pin
            attr = new AttributeDTO();
            attr.setName(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_REQUIRED_TO_CREATE_PIN);
            Boolean[] ReqPin = {aa.findBoolean(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_REQUIRED_TO_CREATE_PIN)};
            attr.setValues(ReqPin);
            attrs.add(attr);

            // the start date
            Calendar cal = Calendar.getInstance();
            Date now = cal.getTime();
            // Date Formatter
            SimpleDateFormat sdf = new SimpleDateFormat(RSAAuthenticationManager8Configuration.DATE_FORMAT);
            Date startDt = null;
            Date endDt = null;

            // the account end date - 1 Year validity
            // Uncomment to set a 1 year validity for the account
            //cal.add(Calendar.YEAR, 1);
            //Date expire = cal.getTime();

            // Temp Start Date
            String TmpStartDtStr = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_START_DATE);
            if (TmpStartDtStr != null) {
                try {
                    startDt = sdf.parse(TmpStartDtStr);
                } catch (ParseException e) {
                    logger.error("Invalid date format for {0}. Expected format: {1}. Provided date: {2}"
                                 , RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_START_DATE 
                                 , RSAAuthenticationManager8Configuration.DATE_FORMAT
                                 , TmpStartDtStr);
                    throw new IllegalArgumentException("Invalid Date Format");
                }
            }
            if (startDt ==null)
                principal.setAccountStartDate(now);
            else
                principal.setAccountStartDate(startDt);
            
            // Temp End Date
            String TmpEndDtStr = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_END_DATE);
            if (TmpEndDtStr != null) {
                try {
                    endDt = sdf.parse(TmpEndDtStr);
                } catch (ParseException e) {
                    logger.error("Invalid date format for {0}. Expected format: {1}. Provided date: {2}"
                                 , RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_END_DATE 
                                 , RSAAuthenticationManager8Configuration.DATE_FORMAT
                                 , TmpEndDtStr);
                    throw new IllegalArgumentException("Invalid Date Format");
                }
            }
            if (endDt != null) {
                principal.setAccountExpireDate(endDt);
                // If an End Date is supplied, it is a Temp user
                attr = new AttributeDTO();
                attr.setName(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMPUSER);
                String[] TempUser = {"true"};
                attr.setValues(TempUser);
                attrs.add(attr);
            }
            
            // Temp User Start Hour
            attr = new AttributeDTO();
            attr.setName(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_START_HOUR);
            String[] TempStartHr = {aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_START_HOUR)};
            attr.setValues(TempStartHr);
            attrs.add(attr);
            
            // Temp User End Hour
            attr = new AttributeDTO();
            attr.setName(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_END_HOUR);
            String[] TempEndHr = {aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_END_HOUR)};
            attr.setValues(TempEndHr);
            attrs.add(attr);
            
            // Secret Word
            attr = new AttributeDTO();
            attr.setName(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_SECRET_WORD);
            String[] Secret = {aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_SECRET_WORD)};
            attr.setValues(Secret);
            attrs.add(attr);

            principal.setAttributes(attrs.toArray(new AttributeDTO[attrs.size()]));
            
            // Prepare Create command
            AddPrincipalsCommand cmd = new AddPrincipalsCommand();
            cmd.setPrincipals(new PrincipalDTO[] { principal });

            logger.info("Creating User Principal...");
            //PrincipalDTO User = new PrincipalDTO();
            try {
                cmd.execute();
                
                // only one user was created, there should be one GUID result
                Uid = cmd.getGuids()[0];
                //User = cmd.getPrincipals()[0];
                principal.setGuid(Uid);
                LoginID = principal.getUserID();
                logger.info("User Principal created with GUID: {0} .", Uid);
                
            } catch (DuplicateDataException e) {
                logger.error("Duplicate user ERROR: " + e.getMessage() + " key: " +e.getMessageKey());
                throw new ConnectorException("Create Principal failure.", e);
            } catch (ObjectInUseException e) {
                // Shouldn't happen on Create, only on Delete
            } catch (DataNotFoundException e) {
                // Shouldn't happen on create, but on find/delete
            } catch (InvalidArgumentException e) {
                logger.error("Invalid Argument ERROR: " + e.getMessage() + " key: " +e.getMessageKey());
                throw new ConnectorException("Create Principal failure.", e);
            } catch (InsufficientPrivilegeException e) {
                logger.error("Insufficient Privileges to create Principal: " + e.getMessage() + " User ID: " + configuration.getCmdclientUser());
                throw new ConnectorException("Create Principal failure.", e);
            } catch (CommandException e) {
                logger.error("An exception was thrown by the RSA command: " + e.getMessage() + " key: " +e.getMessageKey() + " cause: " + e.getCause());
                throw new ConnectorException("Create Principal failure.", e);
            }
            
            // 2- Create AM Account with the new UID
            // This is an add-on to the IMS principal in case any of the following attributes are used/required
            // Uncomment if required...
            /* */
            if ((Uid != null) && (ShellAllowed)) {
                logger.info("Creating AM Principal...");
                AMPrincipalDTO AMprincipal = new AMPrincipalDTO();
                AMprincipal.setGuid(Uid);
                AMprincipal.setDefaultShell(DefShell);
                AMprincipal.setDefaultUserIdShellAllowed(ShellAllowed);
                //AMprincipal.setStaticPassword(RSAAuthenticationManager8Utils.getPlainPassword(Password));
                AMprincipal.setStaticPasswordSet(false);
                //AMprincipal.setWindowsPassword(WinPwd);

                // Perform Account Creation
                AddAMPrincipalCommand AMcmd = new AddAMPrincipalCommand(AMprincipal);
                try {
                    AMcmd.execute();
                    logger.info("AM Principal created.");
                } catch (CommandException e) {
                    logger.error("An exception was thrown by the RSA AM command: " + e.getMessage() + " key: " +
                                 e.getMessageKey() + " cause: " + e.getCause());
                    throw new ConnectorException("Create AM Principal failure.", e);
                }   
            }
            /* */
            
            // ACTION Attributes
            // Group Actions 
            List<Object> Groups = aa.findList(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_GROUPS);
            ArrayList<String> GroupsList = new ArrayList<String>();
            
            // Process Group List
            if ((Uid != null) && (Groups != null)) {
                if (!(Groups.isEmpty())) {
                    // Convert List<Object> to List<String>
                    for (Object grpobj : Groups) {
                        GroupsList.add(grpobj.toString());
                    }
                    logger.info("Linking user {0} to {1} groups...", principal.getUserID(), Groups.size());
                    linkGroups2User(GroupsList.toArray(new String[GroupsList.size()]), principal);
                    //processGroups (User, GroupsList);
                }
            }

            // Token Actions
            ArrayList<String> assignTokenList = new  ArrayList<String>();
            
            // Token 1
            String token1 = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_SN);
            if (token1 != null)
                if (!(token1.isEmpty()))
                    assignTokenList.add(token1);
            // Token 2
            String token2 = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_SN);
            if (token2 != null)
                if (!(token2.isEmpty()))
                    assignTokenList.add(token2);
            // Token 3
            String token3 = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_SN);
            if (token3 != null)
                if (!(token3.isEmpty()))
                    assignTokenList.add(token3);
            if (assignTokenList.size() > 0) {
                logger.info ("Assigning tokens to new user...");
                AssignTokens2User(assignTokenList, Uid);
                logger.info ("{0} tokens succesfully assigned", assignTokenList.size());
            }
   
        }         
        // UNSUPPORTED Object Class
        else {
            logger.error("The RSA connector doesn't support Create for objectClass: {0}",
                         objectClass.getDisplayNameKey());
            throw new UnsupportedOperationException("Connector doesn't support Create for objectClass: " +
                                                    objectClass.getDisplayNameKey());
        }  
        
        return new Uid(LoginID);
        //return new Uid(Uid);
    }

    /**
     * {@inheritDoc}
     */
    // TODO 
    public void delete(final ObjectClass objectClass, final Uid uid,
            final OperationOptions options) {
        //throw new UnsupportedOperationException();
        
        if (objectClass.is("__ACCOUNT__")) {
            String UserId = uid.getUidValue();
            logger.info("Deleting User with ID: " + UserId + "...");

            // Lookup user in RSA to fetch the user's GUID
            PrincipalDTO user;
            try {
                user = lookupUser(UserId);
            } catch (Exception e) {
                logger.error("The User Principal with ID: " + UserId + " doesn't exist on the resource.");
                throw new UnknownUidException ("Unknown UID", e);
            }

            DeletePrincipalsCommand cmd = new DeletePrincipalsCommand();
            cmd.setGuids(new String[] { user.getGuid() });
            cmd.setIdentitySourceGuid(this.connection.getIdSource().getGuid());
            
            try {
                cmd.execute();
                logger.info("User Deleted.");
            } catch (ObjectInUseException e) {
                logger.error("The User Principal object of user: " + UserId + " is currently in use and can't be deleted.");
                throw new RuntimeException ("User Principal Delete Failure", e);
            } catch (InvalidArgumentException e) {
                logger.error("Invalid Argument: " + UserId + ".");
                throw new RuntimeException ("Invalid UID", e);
            } catch (DataNotFoundException e) {
                logger.error("The User Principal with ID: " + UserId + " doesn't exist on the resource.");
                throw new UnknownUidException ("Unknown UID", e);
            } catch (InsufficientPrivilegeException e) {
                logger.error("Insufficient privileges to delete user with ID: " + UserId + ".");
                throw new RuntimeException ("Insufficient privileges.", e);
            } catch (CommandException e) {
                logger.error("An error occured while deleting the User Principal with ID: " + UserId + ".");
                throw new RuntimeException ("An exception occurred...", e);
            }
        }
        // UNSUPPORTED Object Class
        else {
            logger.error("The RSA connector doesn't support Delete for objectClass: {0}",
                         objectClass.getDisplayNameKey());
            throw new UnsupportedOperationException("Connector doesn't support Delete for objectClass: " +
                                                    objectClass.getDisplayNameKey());
        }
        
    }

    // SEARCH
    /**
     * {@inheritDoc}
     */
    public FilterTranslator<Filter> createFilterTranslator(ObjectClass objectClass,
            OperationOptions options) {
        //throw new UnsupportedOperationException();
        return new RSAAuthenticationManager8FilterTranslator();
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options) {
        //throw new UnsupportedOperationException();
        
        // Search for ACCOUNTS
        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            
            if (query == null)
                query = Filter.empty();
            
            if (query.checkEmpty()) {
                logger.info ("Searching for RSA Principals with an empty filter...");
                searchAll = true;
            } else
                logger.info ("Searching for RSA Principals matching query: " + query.toString());

            // Execute search of users matching the provided filter
            PrincipalDTO[] results;

            SearchPrincipalsCommand cmd = new SearchPrincipalsCommand();
            // For paginated search, use:
            //SearchPrincipalsIterativeCommand cmd = new SearchPrincipalsIterativeCommand();
            
            // Search size limit default
            cmd.setLimit(RSAAuthenticationManager8Configuration.SEARCH_LIMIT_DEFAULT);
            
            // Get Search size limit from options, if any, and override the limit
            Map<String,Object> TokenOpts = options.getOptions();
            if (TokenOpts != null) {
                if (TokenOpts.get(RSAAuthenticationManager8Configuration.SEARCH_LIMIT_NAME) != null) {
                    int limit = (Integer) TokenOpts.get(RSAAuthenticationManager8Configuration.SEARCH_LIMIT_NAME);
                    logger.info("Performing Search with Search Page Size limit of: " + limit);
                    cmd.setLimit(limit);
                }
            }
            
            cmd.setIdentitySourceGuid(this.connection.getIdSource().getGuid());
            cmd.setSecurityDomainGuid(this.connection.getDomain().getGuid());
            cmd.setAttributeMask(new String[]{"ALL_INTRINSIC_ATTRIBUTES", "CORE_ATTRIBUTES", "SYSTEM_ATTRIBUTES", "ALL_EXTENDED_ATTRIBUTES"});
            cmd.setFilter(query);
            cmd.setSearchSubDomains(true);
            
            try {
                    cmd.execute();
                    results = cmd.getPrincipals();
                    for (PrincipalDTO principal : results) {
                        if (!(searchAll))
                            logger.info("Building results for user {0}.", principal.getUserID());
                        buildUser(principal, handler);
                    }
            } catch (CommandException e) {
                logger.error("An error occured during search. Cause : " + e.getCause() + " Msg: " + e.getMessage() );
                throw new RuntimeException ("An error occured during Search.", e);
            } 
            searchAll = false;
            
        } else {
            // Unsupported Object Class
            // TODO - Add support for GROUP and TOKEN ObjClasses
            throw new IllegalArgumentException("Unsupported objectclass '" + objectClass.getObjectClassValue() + "'");
        }
    }

    /**
     * Result Builder class to handle the results of a search against RSA. This handler handles 1 search result, and is
     * called by the framework for every row in the query resultset.
     * Calls the ICF 'handle' callback.
     *
     * @param RSAUser a RSA User Principal as returned by an RSA Search command.
     * @param handler The OpenICF Handler to handle these results.
     */
    protected void buildUser(PrincipalDTO RSAUser, ResultsHandler handler) {
        // Get all attributes
        AttributeDTO[] Attribs = extractAttributes(RSAUser);

        int sz = Attribs.length;
        if (!(searchAll))
            logger.info("The RSA Principal has {0} attributes.", sz);

        // Instantiate builder
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(ObjectClass.ACCOUNT);

        // Get Principal UID and set it
        builder.setUid(RSAUser.getUserID());
        builder.setName(RSAUser.getUserID());

        //loop through all attributes
        for (AttributeDTO attr : Attribs) {
            String name = attr.getName();
            Object[] values = attr.getValues();

            if (!(searchAll))
                logger.info("Attrib: {0} = {1}", name, Arrays.toString(values));
            // Skip LoginID since already set
            if (!(name.equalsIgnoreCase(PrincipalDTO.LOGINUID)))
                builder.addAttribute(AttributeBuilder.build(name, values));
            // Uncomment to only return Enabled Principals
            //builder.addAttribute(AttributeBuilder.buildEnabled(true));
        }
        if (!(searchAll)) {
            // do not retrieve extra attributes on search all
            // Groups
            logger.info("Searching for groups...");
            GroupDTO[] groups = getUserGroups(RSAUser.getGuid(), RSAUser.getUserID());
            if (groups != null) {
                if (groups.length > 0) {
                    logger.info("Found {0} groups.", groups.length);
                    ArrayList<String> grpNames = new ArrayList<String>();
                    for (GroupDTO grp : groups) {
                        grpNames.add(grp.getName());
                    }
                    builder.addAttribute(AttributeBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_GROUPS,
                                                                grpNames));
                }
            }
            // Default shell
            logger.info("Fetching the current AM Principal of user {0}...", RSAUser.getUserID());
            LookupAMPrincipalCommand lookupAMCmd = new LookupAMPrincipalCommand();
            lookupAMCmd.setGuid(RSAUser.getGuid());
            AMPrincipalDTO currAMprincipal = null;
            try {
                lookupAMCmd.execute();
                currAMprincipal = lookupAMCmd.getAmp();
            } catch (CommandException e) {
                logger.warn("Failed to fetch the AM principal of user:" + RSAUser.getUserID() + " - " + e.getMessage() 
                            + " - " + e.getMessageKey() + "-" + e.getCause());
            }
            if (currAMprincipal != null) {
                builder.addAttribute(AttributeBuilder.build(AdminResource.DEFAULTSHELL, currAMprincipal.getDefaultShell()));   
            }
        }
        //Build and call handler for the principal
        handler.handle(builder.build());
    }

    // End-SEARCH
    
    /**
     * {@inheritDoc}
     */
    // TODO
    public void test() {
        //throw new UnsupportedOperationException();
        this.connection.test();
    }

    /**
     * {@inheritDoc}
     */
    // TODO
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        //throw new UnsupportedOperationException();

        // Look for requested User
        PrincipalDTO user;
        try {
            logger.info("Looking for user: " + uid.getUidValue() + "...");
            user = lookupUser(uid.getUidValue());
            logger.info("Found user.");
            // this.UserPrincipal = user;
        } catch (Exception e) {
            logger.error("Couldn't find the requested user to update. Error: " + e.getMessage() + " - " + e.getCause());
            throw new UnknownUidException("Unable to update user: " + uid.getUidValue(), e);
        }
        
        /* */// DEBUG
        AttributeDTO[] attribs = user.getAttributes();
        logger.info("attribs length = " + attribs.length);
        for (AttributeDTO attrib : attribs) 
            logger.info("Attrib Name = {0} - Value = {1}", attrib.getName(), attrib.getValues()[0]);
        // END-DEBUG */

        // Read Update Attributes
        AttributesAccessor aa = new AttributesAccessor(replaceAttributes);

        // USER ACCOUNT
        if (objectClass.is("__ACCOUNT__")) {
            HashMap<String, Object> AttribMap = new HashMap<String, Object>();
            // Operation Attributes
            AttribMap.put(PrincipalDTO.PASSWORD, aa.getPassword());
            //AttribMap.put(PrincipalDTO.LOGINUID, uid.getUidValue());
            // Flag indicating whether the principal is enabled or not
            AttribMap.put(PrincipalDTO.ENABLE_FLAG, aa.getEnabled(true));
            //  Flag indicating whether principal is locked out of the system.
            // Maintained by system, do not update
            //AttribMap.put(PrincipalDTO.LOCKOUT_FLAG, aa.findBoolean(PrincipalDTO.LOCKOUT_FLAG));
            // Other Attributes
            AttribMap.put(PrincipalDTO.FIRST_NAME, aa.findString(PrincipalDTO.FIRST_NAME));
            AttribMap.put(PrincipalDTO.LAST_NAME, aa.findString(PrincipalDTO.LAST_NAME));
            AttribMap.put(PrincipalDTO.DESCRIPTION, aa.findString(PrincipalDTO.DESCRIPTION));
            AttribMap.put(PrincipalDTO.MIDDLE_NAME, aa.findString(PrincipalDTO.MIDDLE_NAME));
            AttribMap.put(PrincipalDTO.EMAIL, aa.findString(PrincipalDTO.EMAIL));
            // Flag indicating principal has administrative roles.
            // Maintained by system, do not update.
            //AttribMap.put(PrincipalDTO.ADMINISTRATOR_FLAG, aa.findBoolean(PrincipalDTO.ADMINISTRATOR_FLAG));
            
            // Date Format for all dates:
            SimpleDateFormat sdf = new SimpleDateFormat(RSAAuthenticationManager8Configuration.DATE_FORMAT);
            Date d;
            
            // Date principal's account will expire.
            String endDt = aa.findString(PrincipalDTO.EXPIRATION_DATE);
            if ((endDt==null) || (endDt.isEmpty())) {
                // Try to find the temp end date
                endDt = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_END_DATE);
            }
            if (endDt != null) {
                try {
                    d = sdf.parse(endDt);
                } catch (ParseException e) {
                    logger.error("Invalid date format for EXPIRATION_DATE. Expected format {0}, found {1) ",
                                 RSAAuthenticationManager8Configuration.DATE_FORMAT,
                                 endDt);
                    throw new IllegalArgumentException("Invalid Date Format");
                }
                AttribMap.put(PrincipalDTO.EXPIRATION_DATE, d);
            }
            // Principal's Start Date
            String startDt = aa.findString(PrincipalDTO.START_DATE);
            if ((startDt==null) || (startDt.isEmpty())) {
                // Try to find the temp end date
                startDt = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_START_DATE);
            }
            if (startDt != null) {
                try {
                    d = sdf.parse(startDt);
                } catch (ParseException e) {
                    logger.error("Invalid date format for START_DATE. Expected format {0}, found {1} ",
                                 RSAAuthenticationManager8Configuration.DATE_FORMAT,
                                 startDt);
                    throw new IllegalArgumentException("Invalid Date Format");
                }
                AttribMap.put(PrincipalDTO.START_DATE, d);
            }
            // Date principal will no longer be locked out of the system.
            // Unsupported--
            /*
            try {
                d = sdf.parse(aa.findString(PrincipalDTO.EXPIRE_LOCKOUT_DATE));
            } catch (ParseException e) {
                logger.error ("Invalid date format for EXPIRE_LOCKOUT_DATE. Expected format: " + this.configuration.DATE_FORMAT);
                throw new ConnectorException ("Invalid Date Format");
            }
            AttribMap.put(PrincipalDTO.EXPIRE_LOCKOUT_DATE, d);
            */
            // Flag indicating if principal can be impersonated.
            Boolean impers = aa.findBoolean(PrincipalDTO.IMPERSONATABLE_FLAG);
            if (impers != null)
                AttribMap.put(PrincipalDTO.IMPERSONATABLE_FLAG, impers);
            //  Flag indicating if principal can impersonate others.
            impers = aa.findBoolean(PrincipalDTO.IMPERSONATOR_FLAG);
            if (impers != null)
                AttribMap.put(PrincipalDTO.IMPERSONATOR_FLAG, impers);
            
            // Extended/Custom attribs
            AttribMap.put(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_EMPLOYEE_NB, aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_EMPLOYEE_NB));
            //AttribMap.put(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_ALLOWED_TO_CREATE_PIN, aa.findBoolean(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_ALLOWED_TO_CREATE_PIN));
            //AttribMap.put(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_REQUIRED_TO_CREATE_PIN, aa.findBoolean(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_REQUIRED_TO_CREATE_PIN));
            AttribMap.put(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_SECRET_WORD, aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_SECRET_WORD));

            // Initialize RSA Update Command
            logger.info("Setting ID Source...");
            UpdatePrincipalCommand cmd = new UpdatePrincipalCommand();
            cmd.setIdentitySourceGuid(user.getIdentitySourceGuid());

            // Instantiate RSA Update Object
            UpdatePrincipalDTO updateDTO = new UpdatePrincipalDTO();
            updateDTO.setGuid(user.getGuid());

            // copy the rowVersion to satisfy optimistic locking requirements
            updateDTO.setRowVersion(user.getRowVersion());

            // Modifications:
            List<ModificationDTO> mods = new ArrayList<ModificationDTO>();
            ModificationDTO mod = new ModificationDTO();
            
            // User's current attributes:
            AttributeDTO[] currAttribs = extractAttributes(user);

            // Handle attributes
            for (Map.Entry<String, Object> entry : AttribMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                // call private method for attribute handling
                mod = handleAttribute(key, value, currAttribs);
                try {
                    logger.info("Adding Update Attribute: " + mod.getOperation() + " - " + mod.getName() + " - " +
                                mod.getValues()[0]);
                    mods.add(mod);
                } catch (NullPointerException npe) {
                    if (mod.getOperation() == ModificationDTO.REMOVE_ATTRIBUTE)
                        mods.add(mod);
                    else
                        logger.warn("Mod for Attribute: " + key + " is null, skipping.");
                }
            }

            // set the requested updates into the UpdatePrincipalDTO
            updateDTO.setModifications(mods.toArray(new ModificationDTO[mods.size()]));
            cmd.setPrincipalModification(updateDTO);

            // perform the update
            try {
                cmd.execute();
                logger.info("User Principal Updated.");
            } catch (CommandException e) {
                logger.error("User Principal Update Failure, message: " + e.getMessage() + " Key: " +
                             e.getMessageKey() + " Cause: " + e.getCauses());
                throw new ConnectorException("Update Principal failure.", e);
            }
            
            // TODO - Update Default Shell
            String DefShell = aa.findString(AdminResource.DEFAULTSHELL);
            Boolean ShellAllowed = Boolean.FALSE;
            
            if ((DefShell != null) && (!(DefShell.isEmpty()))) {
                // Default shell value provided, need an AM account.
                ShellAllowed = aa.findBoolean(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_SHELL_ALLOWED);
                if (ShellAllowed == null)
                    // No Default Shell Allowed value provided, assume TRUE since a Shell value is provided.
                    ShellAllowed = Boolean.TRUE;
                // Else use whatever Shell Allowed value is provided

                // Fetch the current AMPrincipal to ensure the update AMPrincipalDTO object
                // has the right row version for optimistic locking
                logger.info("Fetching the current AM Principal of user {0}...", user.getUserID());
                LookupAMPrincipalCommand lookupAMCmd = new LookupAMPrincipalCommand();
                lookupAMCmd.setGuid(user.getGuid());
                AMPrincipalDTO currAMprincipal = null;
                try {
                    lookupAMCmd.execute();
                    currAMprincipal = lookupAMCmd.getAmp();
                } catch (CommandException e) {
                    logger.warn("Failed to fetch the AM principal of user:" + user.getUserID() + " - " + e.getMessage() 
                                + " - " + e.getMessageKey() + "-" + e.getCause());
                    currAMprincipal = new AMPrincipalDTO();
                }

                logger.info("Updating Default Shell on AM Principal...");
                //AMPrincipalDTO AMprincipal = new AMPrincipalDTO();
                //currAMprincipal.setGuid(user.getGuid());
                currAMprincipal.setDefaultShell(DefShell);
                currAMprincipal.setDefaultUserIdShellAllowed(ShellAllowed);
                //currAMprincipal.setRowVersion(user.getRowVersion());
                //AMprincipal.setStaticPassword(RSAAuthenticationManager8Utils.getPlainPassword(Password));
                //AMprincipal.setStaticPasswordSet(false);
                //AMprincipal.setWindowsPassword(WinPwd);

                // Perform Account Creation
                UpdateAMPrincipalCommand AMUpdcmd = new UpdateAMPrincipalCommand(currAMprincipal);
                try {
                    AMUpdcmd.execute();
                    logger.info("AM Principal Updated.");
                } catch (CommandException e) {
                    logger.error("An exception was thrown by the RSA AM command: " + e.getMessage() + " key: " +
                                 e.getMessageKey() + " cause: " + e.getCause());
                    throw new ConnectorException("Update AM Principal failure.", e);
                }
            }
            
            // Update Groups
            List<Object> Groups = aa.findList(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_GROUPS);
            ArrayList<String> GroupsList = new ArrayList<String>();
            // Process Group List
            if (Groups != null) {
                if (!(Groups.isEmpty())) {
                    logger.info("Processing user {0}'s {1} groups...", user.getUserID(), Groups.size());
                    // Convert List<Object> to List<String>
                    for( Object grpobj : Groups ) {
                            GroupsList.add( grpobj.toString() );
                    }
                    //linkGroups2User(GroupsList.toArray(new String[GroupsList.size()]), user);
                    processGroups (user, GroupsList);
                }
            }
            // Update Tokens
            /* */   
            // Token Action Lists
            // Tokens to assign:
            ArrayList<String> assignTokenList = new  ArrayList<String>();
            // Tokens to unassign:
            ArrayList<String> unassignTokenList = new  ArrayList<String>();
            // Tokens to Disable
            ArrayList<String> disableTokenList = new  ArrayList<String>();
            // Tokens to Enable
            ArrayList<String> enableTokenList = new  ArrayList<String>();
            
            // Assign new Tokens
            // If a SN is provided, then assign the token
            String Serial1 = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_SN);
            if (Serial1 != null)
                if (!(Serial1.isEmpty()))
                    assignTokenList.add(Serial1);
            String Serial2 = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_SN);
            if (Serial2 != null)
                if (!(Serial2.isEmpty()))
                    assignTokenList.add(Serial2);
            String Serial3 = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_SN);
            if (Serial3 != null)
                if (!(Serial1.isEmpty()))
                    assignTokenList.add(Serial3);
            // Assign Tokens in bulk
            logger.info("Assigning {0} tokens to user {1}...", assignTokenList.size(), user.getUserID());
            AssignTokens2User(assignTokenList, user.getGuid());
            
            // Get the User's currently assigned Tokens
            // The array is sorted by ascending Serial Number
            TokenDTO[] tokens = getUserTokens(user.getGuid());
            TokenDTO Token1 = null;
            TokenDTO Token2 = null;
            TokenDTO Token3 = null;
            int n = tokens.length;
            logger.info("User has {0} tokens", n);
            if(n== 3) {
                Token1 = tokens[0];
                Token2 = tokens[1];
                Token3 = tokens[2];
            } else if (n == 2) {
                Token1 = tokens[0];
                Token2 = tokens[1];
            } else if (n == 1) {
                Token1 = tokens[0];
            }
            
            // Resynchronize the tokens
            // Token #1
            //Boolean sync1 = aa.findBoolean(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_RESYNC);
            Boolean sync1 = Boolean.valueOf(aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_RESYNC));
            String seq11 =  aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_FIRST_SEQ);
            String seq12 = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_NEXT_SEQ);
            resyncToken (sync1, Token2, seq11, seq12, 1);
            // Token #2
            Boolean sync2 = Boolean.valueOf(aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_RESYNC));
            String seq21 =  aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_FIRST_SEQ);
            String seq22 = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_NEXT_SEQ);
            resyncToken (sync2, Token2, seq21, seq22, 2);
            // Token #3
            Boolean sync3 = Boolean.valueOf(aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_RESYNC));
            String seq31 =  aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_FIRST_SEQ);
            String seq32 = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_NEXT_SEQ);
            resyncToken (sync3, Token3, seq31, seq32, 3);
            
            // Disable tokens
            // Token #1
            String token1DisableStr = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_DISABLED);
            if ((token1DisableStr != null) && (!(token1DisableStr.isEmpty())))  {
                if (Token1 == null) {
                    logger.error("Token Disable/Enable requested but the user doesn't have a Token 1.");
                    throw new IllegalArgumentException ("Token Disable/Enable requested but the user doesn't have a Token 1.");
                } else if (Boolean.valueOf(token1DisableStr))
                    disableTokenList.add(Token1.getId());
                else
                    enableTokenList.add(Token1.getId());
            }
            // Token #2
            String token2DisableStr = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_DISABLED);
            if ((token2DisableStr != null) && (!(token2DisableStr.isEmpty())))  {
                if (Token2 == null) {
                    logger.error("Token Disable/Enable requested but the user doesn't have a Token 2.");
                    throw new IllegalArgumentException ("Token Disable/Enable requested but the user doesn't have a Token 2.");
                }  
                Boolean doDisable = Boolean.valueOf(token2DisableStr);
                if (doDisable) {
                    disableTokenList.add(Token2.getId());
                } 
                else {
                    enableTokenList.add(Token2.getId());
                }
            }
            // Token #3
            String token3DisableStr = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_DISABLED);
            if ((token3DisableStr != null) && (!(token3DisableStr.isEmpty())))  {
                if (Token3 == null) {
                    logger.error("Token Disable/Enable requested but the user doesn't have a Token 3.");
                    throw new IllegalArgumentException ("Token Disable/Enable requested but the user doesn't have a Token 3.");
                } else if (Boolean.valueOf(token3DisableStr)){
                    disableTokenList.add(Token3.getId());
                }
                else {
                    enableTokenList.add(Token3.getId());
                }
            }
            // Perform disable/enable in bulk
            if (disableTokenList.size() > 0) {
                logger.info("Disabling {0} token(s)", disableTokenList.size());
                EnableTokens(disableTokenList.toArray(new String[disableTokenList.size()]), false);
            }
            if (enableTokenList.size() > 0) {
                logger.info("Enabling {0} token(s)", enableTokenList.size());
                EnableTokens(enableTokenList.toArray(new String[enableTokenList.size()]), true);
            }
            
            // Lost Tokens
            // Token #1
            String token1LostStr = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_LOST);
            processLostToken(token1LostStr, Token1, 1);
            // Token #2
            String token2LostStr = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_LOST);
            processLostToken(token2LostStr, Token2, 2);
            // Token #1
            String token3LostStr = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_LOST);
            processLostToken(token3LostStr, Token3, 3);
            
            // Unassign Tokens
            // Token #1
            String doRevokeToken1 = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_UNASSIGN);
            if ((doRevokeToken1 != null) && (!(doRevokeToken1.isEmpty())))  {
                if (Boolean.valueOf(doRevokeToken1)) {
                    // Revoke the token
                    if (Token1 != null) {
                        unassignTokenList.add(Token1.getSerialNumber());
                    } else {
                        logger.error("Token revocation requested but the user doesn't have a Token 1.");
                        throw new IllegalArgumentException ("Token revocation requested but the user doesn't have a Token 1.");
                    }
                }
            }
            // Token #2
            String doRevokeToken2 = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_UNASSIGN);
            if ((doRevokeToken2 != null) && (!(doRevokeToken2.isEmpty())))  {
                if (Boolean.valueOf(doRevokeToken2)) {
                    // Revoke the token
                    if (Token2 != null) {
                        unassignTokenList.add(Token2.getSerialNumber());
                    } else {
                        logger.error("Token revocation requested but the user doesn't have a Token 2.");
                        throw new IllegalArgumentException ("Token revocation requested but the user doesn't have a Token 2.");
                    }
                }
            }
            // Token #3
            String doRevokeToken3 = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_UNASSIGN);
            if ((doRevokeToken3 != null) && (!(doRevokeToken3.isEmpty())))  {
                if (Boolean.valueOf(doRevokeToken3)) {
                    // Revoke the token
                    if (Token3 != null) {
                        unassignTokenList.add(Token3.getSerialNumber());
                    } else {
                        logger.error("Token revocation requested but the user doesn't have a Token 3.");
                        throw new IllegalArgumentException ("Token revocation requested but the user doesn't have a Token 3.");
                    }
                }
            }
            if (unassignTokenList.size() > 0)
                RevokeTokens(unassignTokenList);

            // Update Pin
            // Token #1
            String token1PinMode = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_NEW_PIN_MODE);
            String token1ClearPin = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_CLEAR_PIN);
            updateTokenPin(Token1,token1PinMode,token1ClearPin,1);
            // Token #2
            String token2PinMode = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_NEW_PIN_MODE);
            String token2ClearPin = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_CLEAR_PIN);
            updateTokenPin(Token2,token2PinMode,token2ClearPin,2);
            // Token #3
            String token3PinMode = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_NEW_PIN_MODE);
            String token3ClearPin = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_CLEAR_PIN);
            updateTokenPin(Token3,token3PinMode,token3ClearPin,2);
         
            /*   */

        // TOKEN
        } else if (objectClass.is(RSAAuthenticationManager8Configuration.TOKEN_OBJECTCLASS)) {
            // Get Token List
            List<String> TokenSNs = aa.findStringList(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN_SN_LIST);
            
            // Get Token operation
            Map<String,Object> TokenOpts = options.getOptions();
            // Only 1 option is expected, only read the first one.
            Integer action = (Integer) TokenOpts.entrySet().iterator().next().getValue();
            
            // Perform operation
            switch (action) {
            case 1:
                // RSAAuthenticationManager8Configuration.TOKEN_OP_OPTION_ASSIGN - Assign tokens
                logger.info("Assigning Tokens to user {0}...", uid.getUidValue());
                AssignTokens2User(TokenSNs, user.getGuid());
                logger.info("Tokens succesfully assigned.");
                break;
            case 2:
                // RSAAuthenticationManager8Configuration.TOKEN_OP_OPTION_REVOKE - Revoke tokens
                logger.info("Revoking Tokens...");
                RevokeTokens(TokenSNs);
                logger.info("Tokens succesfully revoked.");
                break;
            case 3:
                // RSAAuthenticationManager8Configuration.TOKEN_OP_OPTION_DISABLE - disable tokens
                logger.info("Disabling Tokens");
                EnableTokens(TokenSerials2GUIDs(TokenSNs), false);
                logger.info("Tokens succesfully disabled.");
                break;
            case 4:
                // RSAAuthenticationManager8Configuration.TOKEN_OP_OPTION_ENABLE - enable tokens
                logger.info("Enabling Tokens");
                EnableTokens(TokenSerials2GUIDs(TokenSNs), true);
                logger.info("Tokens succesfully enabled.");
                break;
            default:
                logger.warn("The requested Token operation: {0} is not supported. Ignoring.", action);
                break;
            }
            
        // GROUP
        } else if (objectClass.is("__GROUP__")) {
            // Get Group Name
            String GroupName = aa.findString(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_GROUP_NAME);
                        
             // Get Group operation
            Map<String,Object> GroupOpts = options.getOptions();
            // Only 1 option is expected, only read the first one.
            Integer action = (Integer) GroupOpts.entrySet().iterator().next().getValue();
            
            // Perform operation
            switch (action) {
            case 1:
                // RSAAuthenticationManager8Configuration.GROUP_OP_OPTION_LINK - Link Group
                logger.info("Linking Group {0} to user {1}...", GroupName, uid.getUidValue());
                linkGroups2User (new String[] {GroupName}, user);
                break;
            case 2:
                // RSAAuthenticationManager8Configuration.GROUP_OP_OPTION_UNLINK - Unlink Group
                logger.info("Unlinking Group {0} from user {1}...", GroupName, uid.getUidValue());
                unlinkGroupsFromUser(new String[] {GroupName}, user);
                break;
            default:
                logger.warn("The requested Group operation: {0} is not supported. Ignoring.", action);
                break;
            }
            
        // UNSUPPORTED Object Class
        } else {
            logger.error("The RSA connector doesn't support Update of objectClass: {0}",
                         objectClass.getDisplayNameKey());
            throw new UnsupportedOperationException("Connector doesn't support Update of objectClass: " +
                                                    objectClass.getDisplayNameKey());
        }

        return uid;
    }
    
    /**
     *
     * {@inheritDoc}
     * @see SchemaOp#schema()
     */
    public Schema schema() {
        if (configuration == null) {
            throw new IllegalStateException("Configuration object has not been set.");
        }
        
        logger.info("Building Schema configuration...");
        // Create Schema
        SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());
        Set<AttributeInfo> attributes = new HashSet<AttributeInfo>();
        
        //USER Objects
        logger.info("USER attributes...");
        // Mandatory Attribute NAME
        AttributeInfoBuilder nmeBuilder = new AttributeInfoBuilder();
        nmeBuilder.setCreateable(true);
        nmeBuilder.setUpdateable(true);
        nmeBuilder.setName(Name.NAME);
        attributes.add(nmeBuilder.build());
        // Mandatory Attribute UID
        AttributeInfoBuilder uidBuilder = new AttributeInfoBuilder();
        uidBuilder.setCreateable(true);
        uidBuilder.setUpdateable(true);
        uidBuilder.setName(Uid.NAME);
        attributes.add(uidBuilder.build());
        
        //Add all RSA User Principal attributes
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.EMAIL));
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.FIRST_NAME));
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.LAST_NAME));
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.MIDDLE_NAME));
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.CERTDN));
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.DESCRIPTION));
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.ADMINISTRATOR_FLAG));
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.EXPIRATION_DATE));
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.IMPERSONATABLE_FLAG));
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.IMPERSONATOR_FLAG));
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.LAST_UPDATED_BY));
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.LAST_UPDATED_ON));
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.LOCKOUT_FLAG));
        attributes.add(AttributeInfoBuilder.build(PrincipalDTO.START_DATE));
        attributes.add(AttributeInfoBuilder.build(AdminResource.DEFAULTSHELL));
        // Custom:
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_EMPLOYEE_NB));
        // Groups is multivalued:
        AttributeInfoBuilder grpBuilder = new AttributeInfoBuilder();
        grpBuilder.setCreateable(true);
        grpBuilder.setUpdateable(true);
        grpBuilder.setMultiValued(true);
        grpBuilder.setName(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_GROUPS);
        attributes.add(grpBuilder.build());
        /* not supported yet:
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_ALLOWED_TO_CREATE_PIN));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_REQUIRED_TO_CREATE_PIN));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMPUSER));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_START_DATE ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_START_HOUR));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_END_DATE));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TEMP_END_HOUR));
        */
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_SECRET_WORD ));
        // Token Custom Attribs:
        // Token 1
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_SN ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_PIN ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_RESYNC ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_FIRST_SEQ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_NEXT_SEQ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_DISABLED));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_LOST));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_NEW_PIN_MODE));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_CLEAR_PIN ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN1_UNASSIGN));
        // Token 2
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_SN));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_PIN ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_RESYNC));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_FIRST_SEQ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_NEXT_SEQ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_DISABLED));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_LOST ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_NEW_PIN_MODE));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_CLEAR_PIN ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN2_UNASSIGN ));
        // Token 3
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_SN));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_PIN ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_RESYNC));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_FIRST_SEQ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_NEXT_SEQ ));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_DISABLED));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_LOST));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_NEW_PIN_MODE));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_CLEAR_PIN));
        attributes.add(AttributeInfoBuilder.build(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN3_UNASSIGN));

        // Operational attributes?
        attributes.add(OperationalAttributeInfos.PASSWORD);
        attributes.add(OperationalAttributeInfos.ENABLE);

        // Build Schema
        schemaBuilder.defineObjectClass(ObjectClass.ACCOUNT_NAME, attributes);

        // TOKEN objects
        logger.info("TOKEN Attributes...");
        attributes = new HashSet<AttributeInfo>();
        AttributeInfoBuilder aib = new AttributeInfoBuilder();
        AttributeInfoBuilder TokenNmeBuilder = new AttributeInfoBuilder();
        TokenNmeBuilder.setCreateable(true);
        TokenNmeBuilder.setUpdateable(true);
        TokenNmeBuilder.setName(Name.NAME);
        attributes.add(TokenNmeBuilder.build());
        // Mandatory Attribute UID
        AttributeInfoBuilder TokenUidBuilder = new AttributeInfoBuilder();
        TokenUidBuilder.setCreateable(true);
        TokenUidBuilder.setUpdateable(true);
        TokenUidBuilder.setName(Uid.NAME);
        attributes.add(TokenUidBuilder.build());
        // Token List
        aib.setMultiValued(true);
        aib.setName(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_TOKEN_SN_LIST);
        attributes.add(aib.build());
        schemaBuilder.defineObjectClass(RSAAuthenticationManager8Configuration.TOKEN_OBJECTCLASS, attributes);
        
        // GROUP objects
        logger.info("GROUP Attributes...");
        attributes = new HashSet<AttributeInfo>();
        aib = new AttributeInfoBuilder();
        AttributeInfoBuilder GroupNmeBuilder = new AttributeInfoBuilder();
        GroupNmeBuilder.setCreateable(true);
        GroupNmeBuilder.setUpdateable(true);
        GroupNmeBuilder.setName(Name.NAME);
        attributes.add(GroupNmeBuilder.build());
        // Mandatory Attribute UID
        AttributeInfoBuilder GroupUidBuilder = new AttributeInfoBuilder();
        GroupUidBuilder.setCreateable(true);
        GroupUidBuilder.setUpdateable(true);
        GroupUidBuilder.setName(Uid.NAME);
        attributes.add(GroupUidBuilder.build());
        // Group Name
        aib.setName(RSAAuthenticationManager8Configuration.CUSTOM_ATTR_GROUP_NAME);
        attributes.add(aib.build());
        schemaBuilder.defineObjectClass(ObjectClass.GROUP_NAME, attributes);

        return schemaBuilder.build();
    }
    
    /**
     *  {@inheritDoc}
     * @see PoolableConnector#checkAlive()
     */
    @Override
    public void checkAlive() {
        String sessionId = null;
        try {
            sessionId = this.connection.getRSASession().getSessionId();
            logger.info("The session with ID: {0} is still alive.", sessionId);
        } catch (Exception e) {
            logger.warn("The connection is not alive...");
            throw new RuntimeException (" RSA Connection is dead.", e);
        }
        if (sessionId == null)
            throw new RuntimeException (" RSA Connection is dead.");
    }
    
    
    /**
     * PRIVATE METHODS
     */
    
    /**
     * Update the token's pin mode and/or clears its pin.
     * 
     * @param token A TokenDTO object encapsulating the token to update.
     * @param pinMode a String representation of a boolean to determine the new pin mode
     * @param clear a String representation of a boolean to determine whether to clear the pin or not
     * @param tokenNb the token number
     */
    private void updateTokenPin (TokenDTO token, String pinMode, String clear, int tokenNb) {
        if (token != null) {
            // Must first fetch the Token object, which includes row versions and all
            // Else update may fail on optimistic lock
            logger.info("Fetching Token...");
            LookupTokenCommand TokCmd = new LookupTokenCommand();
            TokCmd.setGuid(token.getId());
            try {
                TokCmd.execute();
                logger.info("token succefully retrieved.");
            } catch (CommandException e) {
                logger.error ("An error occured while searching for Token with serial: " + token.getSerialNumber());
                throw new RuntimeException ("Token search failure.", e);
            }
            TokenDTO currToken = TokCmd.getToken();
            
            Boolean updated = false;
            if ((pinMode != null) && (!(pinMode.isEmpty()))) {
                // Set New Pin mode
                currToken.setNewPinMode(Boolean.valueOf(pinMode));
                updated = true;
            }
            if ((clear != null) && (!(clear.isEmpty()))) {
                // Set New Pin mode
                currToken.setPinIsSet(Boolean.valueOf(clear));
                updated = true;
            }

            if (updated) {

                // Perform the update
                logger.info("Updating PIN for token {0}.", tokenNb);
                UpdateTokenCommand cmd2 = new UpdateTokenCommand();
                cmd2.setToken(currToken);
                try {
                    cmd2.execute();
                    logger.info("PIN data succesfully updated.");
                } catch (ObjectInUseException e) {
                    logger.error("Can't update the PIN because Token {0} is in use.", tokenNb);
                    throw new RuntimeException("Token in use, can't update.", e);
                } catch (DuplicateDataException e) {
                    logger.error("Can't update the PIN because of duplicate data on Token {0}.", tokenNb);
                    throw new RuntimeException("Duplicate Token data, can't update.", e);
                } catch (InvalidArgumentException e) {
                    logger.error("Invalid data passed for Token {0}.", tokenNb);
                    throw new RuntimeException("Invalid data, can't update.", e);
                } catch (DataNotFoundException e) {
                    logger.error("Can't update the PIN because the supplied Token {0} with serial {1} doesn't exist.",
                                 tokenNb, token.getSerialNumber());
                    throw new RuntimeException("Token doesn't exist, can't update.", e);
                } catch (ConcurrentUpdateException e) {
                    logger.error("Can't update the PIN because Token {0} is being updated concurrently.", tokenNb);
                    throw new RuntimeException("Concurrent update, can't proceed.", e);
                } catch (InsufficientPrivilegeException e) {
                    logger.error("Insuficient privileges to update the PIN of Token {0}.", tokenNb);
                    throw new RuntimeException("Insufficient privileges, can't update.", e);
                } catch (CommandException e) {
                    logger.error("An error occured while updating Token {0}.", tokenNb);
                    throw new RuntimeException("Token update error.", e);
                }
            }
        }
    }
    
    /**
     * Resynchronizes the given Token.
     * 
     * @param isSync a boolean specifying whether a resync is requested.
     * @param token a TokenDTO object encapsulating the token to resync.
     * @param seq1 The First sequence used in token resync.
     * @param seq2 The Next sequence used in token resync
     * @param tokenNb The token number
     */
    private void resyncToken (Boolean isSync, TokenDTO token, String seq1, String seq2, int tokenNb) {
        if ((isSync != null) && (isSync)) {
            // Need first and next sequences to resync the token.
            if ((seq1 == null) || (seq1.isEmpty())) {
                logger.error("Token Resync requested but the Token {0}'s FIRST SEQUENCE is not provided.", tokenNb);
                throw new IllegalArgumentException("Token " + Integer.toString(tokenNb) +
                                                   " Resync requested but the Token's First SEQUENCE is not provided.");
            }
            if ((seq2 == null) || (seq2.isEmpty())) {
                logger.error("Token Resync requested but the Token {0}'s NEXT SEQUENCE is not provided.", tokenNb);
                throw new IllegalArgumentException("Token " + Integer.toString(tokenNb) +
                                                   " Resync requested but the Token's NEXT SEQUENCE is not provided.");
            }
            if (token == null) {
                logger.error("Token Resync requested but the user doesn't have a Token {0}.", tokenNb);
                throw new IllegalArgumentException("Token Resync requested but the user doesn't have a Token" +
                                                   Integer.toString(tokenNb));
            }
            // Resync Tokens
            logger.info("Resync-in Token {0} with SN: {1}", tokenNb, token.getSerialNumber());
            ResynchronizeTokenCommand sync3Cmd = new ResynchronizeTokenCommand(token.getSerialNumber(), seq1, seq2);
            try {
                sync3Cmd.execute();
                logger.info("Succesfully Resync-ed Token {0}...", tokenNb);
            } catch (CommandException e) {
                logger.error("Token " + Integer.toString(tokenNb) + " Resync failure:" + e.getCause() + " - " +
                             e.getMessage() + " - " + e.getMessageKey());
                throw new RuntimeException("Token " + Integer.toString(tokenNb) + " Resync failure.", e);
            }
        }
    }
    /**
     * Processes the Lost status of a given Token
     * @param isLost a STring representation of a boolean - true if the token is in fact lost.
     * @param token the TokenDTO object encapsulating the Token
     * @param tokenNb the token nunmber
     */
    private void processLostToken (String isLost, TokenDTO token, int tokenNb) {
        
        if ((isLost != null) && (!(isLost.isEmpty()))) {
            if (token == null) {
                logger.error("Token Lost operation was requested but the user doesn't have a Token {0}.", tokenNb);
                throw new IllegalArgumentException("Token Lost operation requested but the user doesn't have a Token " + tokenNb);
            }
            // Fetch The Token's Emergency Access object
            logger.info("Looking for Token {0}'s Emergency Access data..", tokenNb);
            LookupTokenEmergencyAccessCommand lostLookupCmd = new LookupTokenEmergencyAccessCommand();
            lostLookupCmd.setGuid(token.getId());
            try {
                lostLookupCmd.execute();
            } catch (CommandException e) {
                logger.error("Token " + Integer.toString(tokenNb) + " Emergency Access data Lookup failure:" + e.getCause() + " - " + e.getMessage() +
                             " - " + e.getMessageKey());
                throw new RuntimeException("Token " + Integer.toString(tokenNb) + " Emergency Access data Lookup failure.", e);
            }
            TokenEmergencyAccessDTO tokenEA = lostLookupCmd.getTokenEmergencyAccess();
            
            // Set the token lost flag
            tokenEA.setTokenLost(Boolean.valueOf(isLost));
            
            // Update the Emergency Data for this token
            UpdateTokenEmergencyAccessCommand lostUpdCmd = new UpdateTokenEmergencyAccessCommand ();
            lostUpdCmd.setTokenEmergencyAccessDTO(tokenEA);
            logger.info("Updating the lost status of Token {0} with SN: {1} to {2}", tokenNb, token.getSerialNumber(), isLost);
            try {
                lostUpdCmd.execute();
                logger.info("Token Lost status succesfully updated.");
            } catch (CommandException e) {
                logger.error("Token " + Integer.toString(tokenNb) + " Emergency Access data Update failure:" + e.getCause() + " - " + e.getMessage() +
                             " - " + e.getMessageKey());
                throw new RuntimeException("Token " + Integer.toString(tokenNb) + " Emergency Access data Update failure.", e);
            }
        }
    }
    
    /**
     * Fetches a user's tokens and returns them.
     * @param UserGuid The RSA GUID of the user who's tokens to fetch.
     * @return array of ListTokenDTO objects for the tokens assigned to the
     *         given principal; an empty array is returned if principal has no
     *         assigned tokens
     */
    private TokenDTO[] getUserTokens (String UserGuid) {
        
        // Fetch the user's tokens
        ArrayList<TokenDTO> tokens = new ArrayList<TokenDTO> ();
        ListTokensByPrincipalCommand cmd = new ListTokensByPrincipalCommand(UserGuid);
        ListTokenDTO[] tokensList = null;
        try {
            cmd.execute();
            tokensList = cmd.getTokenDTOs();
        } catch (DataNotFoundException dne) {
            logger.warn("No tokens found for User with GUID {0}.", UserGuid);
        } catch (CommandException e) {
            logger.error("An error occured while looking for the user tokens");
            throw new RuntimeException ("User token lookup error", e);
        }
        // Sort the Token List
        tokensList = sortTokens (tokensList);
        
        // Convert ListTokens into TokenDTO's. ListTokenDTO is not sufficient to update the Token, so we must first
        // lookup the full token record.
        if (tokensList != null) {
            if (tokensList.length > 0) {
                for (ListTokenDTO lstToken : tokensList) {
                    LookupTokenCommand LookupCmd = new LookupTokenCommand();
                    LookupCmd.setGuid(lstToken.getGuid());
                    try {
                        LookupCmd.execute();
                    } catch (CommandException e) {
                        logger.error("An error occured while converting list tokens into tokens...");
                        throw new RuntimeException("User token convertion error", e);
                    }
                    tokens.add(LookupCmd.getToken());
                }  // End-for
            } // end-if
        } // end-if      
        return tokens.toArray(new TokenDTO[tokens.size()]);
    }
    
    /**
     * Sorts an array of ListTokens on their serial numbers
     * 
     * @param tokensList An array of ListTokenDTO objects
     * @return a sorted array of ListTokenDTO objects. The Tokens with the lowest Serial Number will be first.
     */
    private ListTokenDTO[] sortTokens (ListTokenDTO[] tokensList) {
        
        // Sort the Tokens List by Serial Number
        if (tokensList.length > 1) {
            // First convert to ArrayList
            ArrayList<ListTokenDTO> tokensAryList = new ArrayList<ListTokenDTO>(Arrays.asList(tokensList));
            if (!(searchAll))
                logger.info("Sorting tokens...");
            Collections.sort(tokensAryList, new TokenSerialComparable());
            // Convert back to Array
            tokensList = tokensAryList.toArray(new ListTokenDTO[tokensAryList.size()]);
        }
        return tokensList;
    }
    
    /**
     * Unassign the specified tokens.
     * 
     * @param TokenSerials an Array of Strings storing the Serial Numbers of the Tokens to Un-assign from 
     * their respective users.
     */
    private void RevokeTokens (List<String> TokenSerials ) {
        
        // Convert Serial#'s to GUIDs.
        String[] TokensAry = TokenSerials2GUIDs (TokenSerials);
        
        if (TokensAry.length > 0) {
            logger.info("Unassigning {0} Tokens...", TokensAry.length);
            UnlinkTokensFromPrincipalsCommand UnlinkCmd = new UnlinkTokensFromPrincipalsCommand ();
            UnlinkCmd.setTokenGuids(TokensAry);
            try {
                UnlinkCmd.execute();
                logger.info("Succuesfully unassigned {0} Tokens...", TokensAry.length);
            } catch (CommandException e) {
                logger.error("An error occured while Un-assigning the tokens given tokens.");
                throw new RuntimeException ("Can't unaassign tokens.", e);
            }
        }
    }
    
    /**
     * Assign a set of tokens to a user.
     * 
     * @param TokenSerials an Array of Strings storing the Serial Numbers of the Tokens to assign to the user.
     * @param UserGuid The GUID of the user. This is not the user's Login ID but the RSA GUID for that user.
     * @throws IllegalArgumentException If the provided Serial Number(s) don't match any tokens.
     * @throws RuntimeException If the Assign command fails.
     */
    private void AssignTokens2User (List<String> TokenSerials, String UserGuid) {
        
        // Convert Serial#'s to GUIDs.
        String[] TokensAry = TokenSerials2GUIDs (TokenSerials);
        
        if (TokensAry.length > 0) {
            
            // Enable the tokens
            EnableTokens(TokensAry, true);
            
            // Link tokens
            LinkTokensWithPrincipalCommand cmd2 = new LinkTokensWithPrincipalCommand(TokensAry, UserGuid);
            try {
                cmd2.execute();
                logger.info("Succesfully assigned {0} tokens to the user.", TokensAry.length);
            } catch (CommandException e) {
                logger.error("An error occured while assigning the tokens to user {0}.", UserGuid);
                throw new RuntimeException ("Can't assign tokens.", e);
            }
        } else {
            // Empty tokens array
            logger.warn("No tokens were found to assign, no action was performed.");
        }
    }
    
    /**
     * Converts an input list of Token serial numbers into an array of their corresponding GUIDs read from RSA AM.
     * 
     * @param TokenSerials the list of Token Serial Numbers to convert.
     * @return an Array of Strings.
     */
    private String[] TokenSerials2GUIDs (List<String> TokenSerials) {
        
        // Array of Token GUIDs to process
        ArrayList<String> tokens = new ArrayList<String> ();

        // Loop through all provided Token SN's
        for (String TokenSN : TokenSerials) {
            // First fetch the Token matching the given Serial Number.
            TokenDTO token = null;
            LookupTokenCommand cmd = new LookupTokenCommand();
            cmd.setSerialNumber(TokenSN);
            try {
                cmd.execute();
                token = cmd.getToken();
            } catch (CommandException e) {
                logger.error("The provided Token Serial Number {0} doesn't match any tokens, or an error occured while fetching the token.", TokenSN);
                throw new IllegalArgumentException ("Can't fetch token.", e);
            }
            if (token != null) {
                // Get the Token's GUID
                String TokenGUID = token.getId();
                // Add token GUID to Array of GUIDs to assign to user
                tokens.add(TokenGUID);
            } // End if
        } // End for
        
        // Convert to Array
        return tokens.toArray(new String[tokens.size()]);
    }
    
    /**
     * Enables a set of Tokens
     * 
     * @param tokens an array of Strings containing the GUIDS of the tokens to enable or disable.
     */
    private void EnableTokens (String[] tokens, Boolean enable) {
        logger.info("Processing enablement/disablement of {0} Tokens with status = {1}...",  tokens.length, Boolean.toString(enable));
        EnableTokensCommand EnableCmd = new EnableTokensCommand();
        EnableCmd.setTokenGuids(tokens);
        EnableCmd.setEnable(enable);
        try {
            EnableCmd.execute();
            logger.info("{0} Tokens processed succesfully.", tokens.length);
        } catch (CommandException e) {
            logger.error("An error occured while processing the Enablement of the tokens.");
            throw new RuntimeException ("Can't process the given tokens.", e);
        }
    }

    /**
     * Lookup a security domain by name Searches all levels of the security
     * domains hierachy
     * @throws Exception
     */
    private SecurityDomainDTO lookupSecurityDomain(String name) throws Exception {
        SearchSecurityDomainCommand cmd = new SearchSecurityDomainCommand();
        cmd.setFilter(Filter.equal(SecurityDomainDTO.NAME_ATTRIBUTE, name));
        cmd.setLimit(1);

        // in order to search all levels we set searchbase to "*"
        cmd.setSearchBase("*");
        cmd.setSearchScope(SecurityDomainDTO.SEARCH_SCOPE_SUB);
        cmd.execute();

        if (cmd.getSecurityDomains().length == 0) {
            throw new Exception("Could not find security domain " + name);
        }

        return cmd.getSecurityDomains()[0];
    }
    
    /**
     * Lookup an agent by name.
     *
     * @param name the agent name to lookup
     * @return the GUID of the agent
     * @throws Exception
     */
    private ListAgentDTO lookupAgent(String name) throws Exception {
        SearchAgentsCommand cmd = new SearchAgentsCommand();
        cmd.setFilter(Filter.equal(AgentConstants.FILTER_HOSTNAME, name));
        cmd.setLimit(1);
        cmd.setSearchBase(connection.getIdSource().getGuid());
        // the scope flags are part of the SecurityDomainDTO
        cmd.setSearchScope(SecurityDomainDTO.SEARCH_SCOPE_SUB);

        cmd.execute();
        if (cmd.getAgents().length < 1) {
            throw new Exception("ERROR: Unable to find agent " + name + ".");
        }

        return cmd.getAgents()[0];
    }
    
    /**
     * Lookup a user by login UID.
     *
     * @param userId the user login UID
     * @return the user record.
     * @throws Exception
     */
    private PrincipalDTO lookupUser(String userId) throws Exception {
        SearchPrincipalsCommand cmd = new SearchPrincipalsCommand();

        // create a filter with the login UID equal condition
        cmd.setFilter(Filter.equal(PrincipalDTO.LOGINUID, userId));
        cmd.setSystemFilter(Filter.empty());
        cmd.setLimit(1);
        cmd.setIdentitySourceGuid(connection.getIdSource().getGuid());
        cmd.setSecurityDomainGuid(connection.getDomain().getGuid());
        cmd.setGroupGuid(null);
        cmd.setOnlyRegistered(true);
        cmd.setSearchSubDomains(true);
        cmd.setAttributeMask(new String[]{"ALL_INTRINSIC_ATTRIBUTES", "CORE_ATTRIBUTES", "SYSTEM_ATTRIBUTES", "ALL_EXTENDED_ATTRIBUTES"}); //"ALL_ATTRIBUTES"

        cmd.execute();

        if (cmd.getPrincipals().length < 1) {
            throw new UnknownUidException("Unable to find user " + userId + ".");
        }

        return cmd.getPrincipals()[0];
    }
    
    /**
     * Processes the Principal's given attribute name/value to create a RSA Modification DTO row as follows:
     * <ul>
     *  <li> Replace all of the current values of that attribute in the target object with the provided values of that attribute.</li>
     *  <li> If the target object does not currently contain an attribute that the input set contains, 
     *  then add this attribute (along with the provided values) to the target object.</li>
     *  <li> If the value of an attribute in the input set is null, then remove that attribute from the target 
     *  object entirely </li>
     *  </ul>
     *  
     * @param AttribName
     * @param AttribVal
     * @return
     */
    private ModificationDTO handleAttribute (String AttribName, Object AttribVal, AttributeDTO[] UserAttribs) {
        ModificationDTO ModRow = new ModificationDTO();

        logger.info("\n Handing attribute : " + AttribName + " with value: " + AttribVal);

        // Do NOT remove these attributes:
        String[] doNotRemove = new String[] {
            PrincipalDTO.PASSWORD, PrincipalDTO.LOGINUID, PrincipalDTO.ADMINISTRATOR_FLAG };

        if (AttribVal == null) {
            // Remove Attribute from target entry except the above
            if (!(Arrays.asList(doNotRemove).contains(AttribName))) {
                ModRow.setOperation(ModificationDTO.REMOVE_ATTRIBUTE);
                ModRow.setName(AttribName);
            }
        } else {
            // Check if attribute exists on target entry
            Boolean found = false;
            // Search for attribute
            for (int i = 0; i < UserAttribs.length; i++) {
                if (UserAttribs[i].getName().equals(AttribName)) {
                    // found attribute
                    found = true;
                    break;
                }
            }
            if (AttribName == PrincipalDTO.PASSWORD) {
                // Special case for pwd: decrypt the Guarded String
                AttribVal = RSAAuthenticationManager8Utils.getPlainPassword((GuardedString) AttribVal);
            }
            if (found) {
                // Found: Replace attribute value
                ModRow.setOperation(ModificationDTO.REPLACE_ATTRIBUTE);
                ModRow.setName(AttribName);
                ModRow.setValues(new Object[] { AttribVal });
            } else {
                // Not found: add the attribute
                ModRow.setOperation(ModificationDTO.ADD_ATTRIBUTE);
                ModRow.setName(AttribName);
                ModRow.setValues(new Object[] { AttribVal });
            } // end Found
        } // end attrib == null
        return ModRow;
    }
    
    /**
     * The RSA SDK doesn't provide a convenience method to extract all attributes from a Principal DTO object. The
     * Principal's attributes must be read one by one by their authenticationors.
     * This methods extracts all of a User Principal's attributes and stores them into an array of AttributeDTO's.
     * 
     * @param User the Principal's DTO object
     * @return and array of AttributeDTO encapsulating the user's attributes.
     */
    private AttributeDTO[] extractAttributes (PrincipalDTO User) {

        // Extended attributes:
        AttributeDTO[] attribs = User.getAttributes();
        logger.info("====== Extracting Attribs - attribs size = {0}", attribs.length);
        ArrayList<AttributeDTO> AttribList = new ArrayList<AttributeDTO>(Arrays.asList(attribs));
        logger.info("====== Attribs List size = {0}", AttribList.size());

        // Core Attributes:
        AttributeDTO loginAttr = new AttributeDTO();
        loginAttr.setName(PrincipalDTO.LOGINUID);
        String[] id = { User.getUserID() };
        loginAttr.setValues(id);
        AttribList.add(loginAttr);

        AttributeDTO emailAttr = new AttributeDTO();
        emailAttr.setName(PrincipalDTO.EMAIL);
        String[] email = { User.getEmail() };
        emailAttr.setValues(email);
        AttribList.add(emailAttr);

        AttributeDTO fnameAttr = new AttributeDTO();
        fnameAttr.setName(PrincipalDTO.FIRST_NAME);
        String[] fname = { User.getFirstName() };
        fnameAttr.setValues(fname);
        AttribList.add(fnameAttr);

        AttributeDTO lnameAttr = new AttributeDTO();
        lnameAttr.setName(PrincipalDTO.LAST_NAME);
        String[] lname = { User.getLastName() };
        lnameAttr.setValues(lname);
        AttribList.add(lnameAttr);

        AttributeDTO mnameAttr = new AttributeDTO();
        mnameAttr.setName(PrincipalDTO.MIDDLE_NAME);
        String[] mname = { User.getMiddleName() };
        mnameAttr.setValues(mname);
        AttribList.add(mnameAttr);

        AttributeDTO certdnAttr = new AttributeDTO();
        certdnAttr.setName(PrincipalDTO.CERTDN);
        String[] cdn = { User.getCertificateDN() };
        certdnAttr.setValues(cdn);
        AttribList.add(certdnAttr);

        // System Attributes:
        AttributeDTO descrAttr = new AttributeDTO();
        descrAttr.setName(PrincipalDTO.DESCRIPTION);
        String[] descr = { User.getDescription() };
        descrAttr.setValues(descr);
        AttribList.add(descrAttr);

        AttributeDTO adminAttr = new AttributeDTO();
        adminAttr.setName(PrincipalDTO.ADMINISTRATOR_FLAG);
        Boolean[] admin = { User.isAdminRole() };
        adminAttr.setValues(admin);
        AttribList.add(adminAttr);

        AttributeDTO enbAttr = new AttributeDTO();
        enbAttr.setName(PrincipalDTO.ENABLE_FLAG);
        Boolean[] enb = { User.isEnabled() };
        enbAttr.setValues(enb);
        AttribList.add(enbAttr);

        // Date Format for dates
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

        AttributeDTO expDtAttr = new AttributeDTO();
        expDtAttr.setName(PrincipalDTO.EXPIRATION_DATE);
        Date expDt = User.getAccountExpireDate();
        if (expDt != null) {
            String[] expDtStr = { df.format(expDt) };
            expDtAttr.setValues(expDtStr);
            AttribList.add(expDtAttr);
        }

        AttributeDTO impAttr = new AttributeDTO();
        impAttr.setName(PrincipalDTO.IMPERSONATABLE_FLAG);
        Boolean[] imp = { User.isCanBeImpersonated() };
        impAttr.setValues(imp);
        AttribList.add(impAttr);

        AttributeDTO trustAttr = new AttributeDTO();
        trustAttr.setName(PrincipalDTO.IMPERSONATOR_FLAG);
        Boolean[] trust = { User.isTrustToImpersonate() };
        trustAttr.setValues(trust);
        AttribList.add(trustAttr);

        AttributeDTO adminFlagAttr = new AttributeDTO();
        adminFlagAttr.setName(PrincipalDTO.ADMINISTRATOR_FLAG);
        Boolean[] adminFlag = { User.isAdminRole() };
        adminFlagAttr.setValues(adminFlag);
        AttribList.add(trustAttr);

        AttributeDTO modAttr = new AttributeDTO();
        modAttr.setName(PrincipalDTO.LAST_UPDATED_BY);
        String[] mod = { User.getLastModifiedBy() };
        modAttr.setValues(mod);
        AttribList.add(modAttr);

        AttributeDTO modDtAttr = new AttributeDTO();
        modDtAttr.setName(PrincipalDTO.LAST_UPDATED_ON);
        Date modDt = User.getLastModifiedOn();
        if (modDt != null) {
            String[] modDtStr = { df.format(modDt) };
            modDtAttr.setValues(modDtStr);
            AttribList.add(modDtAttr);
        }

        AttributeDTO lockAttr = new AttributeDTO();
        lockAttr.setName(PrincipalDTO.LOCKOUT_FLAG);
        Boolean[] lock = { User.isLockoutStatus() };
        lockAttr.setValues(lock);
        AttribList.add(lockAttr);

        AttributeDTO startAttr = new AttributeDTO();
        startAttr.setName(PrincipalDTO.START_DATE);
        Date start = User.getAccountStartDate();
        if (start != null) {
            String[] startStr = { df.format(start) };
            startAttr.setValues(startStr);
            AttribList.add(startAttr);
        }

        // Extended Attributes
        /* *
        AttributeDTO[] extendedAttrs = User.getAttributes();
        if (!(searchAll))
            logger.info("Extracted {0} extended attributes. Adding...", extendedAttrs.length);
        for (AttributeDTO extendedAttr : extendedAttrs) {
            AttribList.add(extendedAttr);
        }
        /* */

        // User's Token Attributes
        // TokenDTO[] tokens = getUserTokens(User.getGuid());
        // Fetch the user's tokens
        //ArrayList<TokenDTO> tokens = new ArrayList<TokenDTO> ();
        if (!(searchAll))
            logger.info("Fetching {0}'s assigned tokens...", User.getUserID());
        ListTokensByPrincipalCommand cmd = new ListTokensByPrincipalCommand(User.getGuid());
        ListTokenDTO[] tokensList = null;
        try {
            cmd.execute();
            tokensList = cmd.getTokenDTOs();
            if (!(searchAll))
                logger.info("Found {0} tokens.",tokensList.length);
        } catch (DataNotFoundException dne) {
            logger.warn("No tokens found for User with GUID {0}.", User.getGuid());
        } catch (CommandException e) {
            logger.error("An error occured while looking for the user tokens");
            throw new RuntimeException("User token lookup error", e);
        }

        if (tokensList != null) {
            // Sort the tokens by serial numbers
            /* */
            tokensList = sortTokens (tokensList);
            /* */
            // Process results
            for (int i = 0; i < tokensList.length; i++) {
                ListTokenDTO token = tokensList[i];
                // Build custom attribute name using the array index as token identifier
                String TokenPrefix = "token";

                // Serial #
                //logger.info("---- Token serial #: {0}",token.getSerialNumber());
                String serial = new StringBuffer(TokenPrefix).append(Integer.toString(i+1)).append("SerialNumber").toString();
                AttributeDTO serialAttr = new AttributeDTO();
                serialAttr.setName(serial);
                String[] sn = { token.getSerialNumber() };
                serialAttr.setValues(sn);
                AttribList.add(serialAttr);

                // Token Lost
                //logger.info("---- Token Lost #: {0}",token.getTokenLost());
                String lost = new StringBuffer(TokenPrefix).append(Integer.toString(i+1)).append("Lost").toString();
                AttributeDTO lostAttr = new AttributeDTO();
                lostAttr.setName(lost);
                String[] tokenLost = { Boolean.toString(token.getTokenLost()) };
                lostAttr.setValues(tokenLost);
                AttribList.add(lostAttr);

                // New Pin Mode
                //logger.info("---- Token New Pin Mode #: {0}",token.getNewPinMode());
                String newPinMode = new StringBuffer(TokenPrefix).append(Integer.toString(i+1)).append("NewPinMode").toString();
                AttributeDTO newPinModeAttr = new AttributeDTO();
                newPinModeAttr.setName(newPinMode);
                String[] newPinModeAry = { Boolean.toString(token.getNewPinMode()) };
                newPinModeAttr.setValues(newPinModeAry);
                AttribList.add(newPinModeAttr);

                // Disabled?
                //logger.info("---- Token disabled #: {0}",token.getEnable());
                String disabled = new StringBuffer(TokenPrefix).append(Integer.toString(i+1)).append("Disabled").toString();
                AttributeDTO disabledAttr = new AttributeDTO();
                disabledAttr.setName(disabled);
                Boolean disable = !(token.getEnable()); // Return a DISABLED flag
                String[] disabledAry = { Boolean.toString(disable) };
                disabledAttr.setValues(disabledAry);
                AttribList.add(disabledAttr);
            } // End-for
        } // End-if TokenList
        
        return AttribList.toArray(new AttributeDTO[AttribList.size()]);
    }

    
    /**
     * Lookup a group by name.
     *
     * @param name the name of the group to lookup
     * @return the GUID of the group
     * @throws Exception
     */
    private GroupDTO lookupGroup(String name) {
        
        SearchGroupsCommand cmd = new SearchGroupsCommand();
        cmd.setFilter(Filter.equal(GroupDTO.NAME, name));
        cmd.setSystemFilter(Filter.empty());
        cmd.setLimit(1);
        cmd.setIdentitySourceGuid(this.connection.getIdSource().getGuid());
        cmd.setSecurityDomainGuid(this.connection.getDomain().getGuid());
        cmd.setSearchSubDomains(true);
        cmd.setGroupGuid(null);

        try {
            cmd.execute();
        } catch (DataNotFoundException e) {
            logger.error ("Unable to find group {0}.", name);
            throw new RuntimeException ("Unable to find group.", e);
        } catch (InvalidArgumentException e) {
            logger.error ("The provide group name: {0} is invalid.", name);
            throw new RuntimeException ("Invalid group name.", e);
        } catch (InsufficientPrivilegeException e) {
            logger.error ("Insufficient privileges to find group {0}.", name);
            throw new RuntimeException ("Insufficient privileges.", e);
        } catch (CommandException e) {
            logger.error ("An error occured while looking for group {0}.", name);
            throw new RuntimeException ("Unable to find group.", e);
        }

        if (cmd.getGroups().length < 1) {
            throw new UnknownUidException("Unable to find group " + name + ".");
        }
        return cmd.getGroups()[0];
    }
    
    /**
     * Fetch all of a RSA Principal's groups.
     * 
     * @param userGUID the user Principal's GUID
     * @param userID The user princpal's loging ID
     * @return an Array of GroupDTO objects encapsulating the user's groups
     */
    private GroupDTO[] getUserGroups (String userGUID, String userID) {
        GroupDTO[] groups;
        if (!(searchAll))
            logger.info("Fetching the list of groups user {0} in Security Domain {1} is a member of..."
                    , userID, this.connection.getIdSource().getGuid());
        GetPrincipalGroupsCommand cmd = new GetPrincipalGroupsCommand();
        //GetUserGroupsCommand cmd = new GetUserGroupsCommand();
        cmd.setGuid(userGUID);
        cmd.setIdentitySourceGuid(this.connection.getIdSource().getGuid());
        //cmd.setSecurityDomainID(this.connection.getDomain().getGuid());
        
        try {
            cmd.execute();
            groups = cmd.getGroups();
            if (!(searchAll))
                logger.info("Succesfully retrieved {0}'s {1) groups.", userID, groups.length);
        } catch (InvalidArgumentException e) {
            logger.error("The provided parameters are invalid or data is missing. " + e.getCause() + "\n" + e.getMessage()
                         + "\n" + e.getMessageKey());
            throw new RuntimeException("Invalid parameters.", e);
        } catch (InsufficientPrivilegeException e) {
            logger.error("Insufficient privileges to search user {1}'s groups.", userID);
            throw new RuntimeException("Insufficient privileges.", e);
        } catch (DataNotFoundException e) {
            logger.error("Unable to find user {0}.", userID);
            throw new RuntimeException("Unable to find user.", e);
        } catch (CommandException e) {
            logger.error("An error occured while searching for {0}'s groups.", userID);
            throw new RuntimeException("Unable to search for the user's groups.", e);
        }
        
        return groups;
    }
    
    /**
     * Processes a set of groups for a given user: makes the user a member of the groups which names
     * are provided. I.e., Links the user to those groups he's not already linked to, unlinks the user from the groups
     * not listed.
     * 
     * @param user The user's Principal object.
     * @param GroupNames The list of group names to link to the user to.
     */
    private void processGroups (PrincipalDTO user, ArrayList<String> GroupNames) {

        // Get the list of groups the user is currently a member of:
        GroupDTO[] groups = getUserGroups(user.getGuid(), user.getUserID());

        // CReate List of current User gorups
        ArrayList<String> currGrpNames = new ArrayList<String>();
        for (GroupDTO grp : groups) {
            currGrpNames.add(grp.getName());
        }

        if ((GroupNames == null) || (GroupNames.isEmpty())) {
            // If the user has no current groups: do nothing.
            if (currGrpNames.size() > 0) {
                // No group names supplied: Remove all current memberships
                logger.info("Unlinking {0} groups from fo user {1}...", currGrpNames.size(), user.getUserID());
                unlinkGroupsFromUser(currGrpNames.toArray(new String[currGrpNames.size()]), user);
            }
            
        } else if (currGrpNames.size() > 0) {
            // Some group names were provided, AND the user has current memberships.
            
            // Compare the user's current group membership to the desired/provided list.
            // Groups to add:
            ArrayList<String> AddGroups = new ArrayList<String>();
            for (String name : GroupNames) {
                if (!(currGrpNames.contains(name)))
                    AddGroups.add(name);
            }
            // Groups to remove:
            ArrayList<String> DelGroups = new ArrayList<String>();
            for (String name : currGrpNames) {
                if (!(GroupNames.contains(name)))
                    DelGroups.add(name);
            }
            if (!(AddGroups.isEmpty())) {
                // Add groups
                logger.info("Linking {0} groups to user {1}...", AddGroups.size(), user.getUserID());
                linkGroups2User(AddGroups.toArray(new String[AddGroups.size()]), user);
            }
            if (!(DelGroups.isEmpty())) {
                // Remove groups
                logger.info("Unlinking {0} groups from fo user {1}...", DelGroups.size(), user.getUserID());
                unlinkGroupsFromUser(DelGroups.toArray(new String[DelGroups.size()]), user);
            }
        } else {
            // The user currently has no groups AND Group names were supplied: add them all:
            logger.info("Linking {0} groups to user {1}...", GroupNames.size(), user.getUserID());
            linkGroups2User(GroupNames.toArray(new String[GroupNames.size()]), user);
        }

    }
    
    /**
     * Transforms an array of Group names into an array of Group GUIDs by looking the group object up in RSA.
     * 
     * @param GrpNames An array of Group Name Strings
     * @return An Array of GUID Strings
     */ 
    private String[] groupNames2Guids (String[] GrpNames) {
        // Build array of Group Guids from the array or Group Names
        ArrayList<String> GroupGuidsList = new ArrayList<String> ();
        for (String GroupName : GrpNames) {
            GroupDTO group = lookupGroup(GroupName);
            GroupGuidsList.add(group.getGuid());
        }
        return GroupGuidsList.toArray(new String[GroupGuidsList.size()]);
    }
    
    /**
     * Links a RSA group to a user.
     * 
     * @param GroupNames The list of RSA Group names to link.
     * @param user The User Principal object.
     */
    private void linkGroups2User (String [] GroupNames, PrincipalDTO user) {
        
        LinkGroupPrincipalsCommand cmd = new LinkGroupPrincipalsCommand();
        
        // Build array of Group Guids from the array or Group Names
        String[] GroupGuids = groupNames2Guids (GroupNames);
        // Parameters
        cmd.setGroupGuids(GroupGuids);
        String[] users = { user.getGuid() };
        cmd.setPrincipalGuids(users);
        logger.info("Principal set {0}.", cmd.getPrincipalGuids()[0]);
        cmd.setIdentitySourceGuid(this.connection.getIdSource().getGuid());
        // Run command
        try {
            cmd.execute();
            logger.info(" {0} Group(s) succesfully linked.", GroupGuids.length);
        } catch (InvalidArgumentException e) {
            logger.error ("The provided group names or UserID {0} is invalid.", user.getUserID());
            throw new RuntimeException ("Invalid Parameters.", e);
        } catch (InsufficientPrivilegeException e) {
            logger.error ("Insufficient privileges to link groups to user {0}.", user.getUserID());
            throw new RuntimeException ("Insufficient privileges.", e);
        } catch (DataNotFoundException e) {
            logger.error ("Unable to find groups or user {1}.", user.getUserID());
            throw new RuntimeException ("Unable to find group or user.", e);
        } catch (DuplicateDataException e) {
            logger.error ("Groups already linked to user {0}.", user.getUserID());
            throw new RuntimeException ("Group already linked to user.", e);
        } catch (CommandException e) {
            logger.error ("An error occured while linking to user {0}.", user.getUserID());
            throw new RuntimeException ("Unable to link group.", e);
        }
    }
    
    /**
     * Unlink a set of Groups from as User
     * 
     * @param GroupNames The list of group names of the RSA group to unlink
     * @param user The User's Principal object
     */
    private void unlinkGroupsFromUser (String[] GroupNames, PrincipalDTO user){
        
        UnlinkGroupPrincipalsCommand UnlinkCmd = new UnlinkGroupPrincipalsCommand();
        
        // Build array of Group Guids from the array or Group Names
        String[] GroupGuids = groupNames2Guids (GroupNames);
        // Perform opration
        UnlinkCmd.setGroupGuids(GroupGuids);
        UnlinkCmd.setPrincipalGuids(new String[] { user.getGuid() });
        UnlinkCmd.setIdentitySourceGuid(this.connection.getIdSource().getGuid());
        try {
            UnlinkCmd.execute();
            logger.info("Group succesfully unlinked {0} groups.", GroupGuids.length);
        } catch (InvalidArgumentException e) {
            logger.error ("The provided group(s)or UserID {0} is invalid.", user.getUserID());
            throw new RuntimeException ("Invalid group or user name.", e);
        } catch (InsufficientPrivilegeException e) {
            logger.error ("Insufficient privileges to unlink groups from user {0}.", user.getUserID());
            throw new RuntimeException ("Insufficient privileges.", e);
        } catch (DataNotFoundException e) {
            logger.error ("Unable to find groups or user {0s}.", user.getUserID());
            throw new RuntimeException ("Unable to find group or user.", e);
        } catch (CommandException e) {
            logger.error ("An error occured while unlinking groups from user {0}.", user.getUserID());
            throw new RuntimeException ("Unable to unlink groups.", e);
        }
    }

    /**
    * Embedded classes
    */
    
    /**
     * Implements the comparable interface to compare RSA Tokens against each other based on their Serial Numbers.
     * Used for sorting tokens.
     */
    public class TokenSerialComparable implements Comparator<ListTokenDTO>{
     
        @Override
        public int compare(ListTokenDTO token1, ListTokenDTO token2) {
            return token1.getSerialNumber().compareTo(token2.getSerialNumber());
        }
    } 
}
