/*
 *
 * Copyright (c) 2010 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 *
 * $Id$
 */
package org.forgerock.openicf.tam;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.PredefinedAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;

import com.tivoli.pd.jadmin.PDAdmin;
import com.tivoli.pd.jadmin.PDGroup;
import com.tivoli.pd.jadmin.PDSSOCred;
import com.tivoli.pd.jadmin.PDSSOResource;
import com.tivoli.pd.jadmin.PDSSOResourceGroup;
import com.tivoli.pd.jadmin.PDUser;
import com.tivoli.pd.jutil.PDContext;
import com.tivoli.pd.jutil.PDException;
import com.tivoli.pd.jutil.PDMessage;
import com.tivoli.pd.jutil.PDMessages;
import com.tivoli.pd.jutil.PDRgyGroupName;
import com.tivoli.pd.jutil.PDRgyUserName;
import com.tivoli.pd.nls.pdbjamsg;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;

/**
 * Main implementation of the Tivoli Access Manager Connector
 * 
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0
 */
@ConnectorClass(displayNameKey = "TAM", configurationClass = TAMConfiguration.class)
public class TAMConnector implements PoolableConnector, AuthenticateOp, CreateOp, SchemaOp, TestOp, DeleteOp, UpdateOp, SearchOp<String> {

    /**
     * Setup logging for the {@link TAMConnector}.
     */
    private static final Log log = Log.getLog(TAMConnector.class);
    private static int count = 0;
    private PDContext ctxt;
    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link TAMConnector#init}.
     */
    private TAMConfiguration configuration;
    /**
     * Place holder for the {@link Schema} create in the schema() method
     * {@link TAMConnector#schema}.
     */
    private Schema schema;
    // Resource Attributes
    public static final String ATTR_REGISTRY_NAME = "registryName";
    public static final String ATTR_FIRST_NAME = "firstname";
    public static final String ATTR_LAST_NAME = "lastname";
    public static final String ATTR_SSO_USER = "ssoUser";
    public static final String ATTR_PASSWORD_POLICY = "passwordPolicy";
    public static final String ATTR_EXPIRE_PASSWORD = "expirePassword";
    public static final String ATTR_DELETE_FROM_REGISTRY = "deleteFromRegistry";
    public static final String ATTR_SYNC_GSO_CREDENTIALS = "syncGSOCredentials";
    public static final String ATTR_IMPORT_FROM_REGISTRY = "importFromRegistry";
    public static final String ATTR_GROUP_MEMBERS = "members";
    public static final String ATTR_GSO_WEB_CREDENTIALS = "gsoWebCredentials";
    public static final String ATTR_GSO_GROUP_CREDENTIALS = "gsoGroupCredentials";
    public static final String TYPE_GSO_GROUP_RESOURCE = "GSOGroupResource";
    public static final String TYPE_GSO_RESOURCE = "GSOWebResource";
    public static final String TOKEN_GSO_RESOURCE = "|:";

    /**
     * Gets the Configuration context for this connector.
     */
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     * 
     * @see Connector#init
     */
    /** Initialize the PDAdmin API.
     *
     * Before using the administration API in a Java application, the PDAdmin
     * object must be initialized. This initialization is accomplished by
     * calling the PDAdmin.initialize() method.
     * 
     * @see org.identityconnectors.framework.spi.Connector#init(org.identityconnectors.framework.spi.Configuration)
     */
    public void init(Configuration config) {
        try {
            synchronized (TAMConnector.class) {
                configuration = (TAMConfiguration) config;
                try {
                    if (count == 0) {
                        PDMessages msgs = new PDMessages();
                        log.info("Initializing PDAdmin.");
                        PDAdmin.initialize(TAMConfiguration.CONNECTOR_NAME, msgs);
                        processMessages("init", msgs);
                    }
                    count++;
                }
                catch (PDException e) {
                    convertPDExceptionToConnectorException(e);
                }
                createPDContext();
            }
        }
        catch (Exception e) {
            dispose();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new ConnectorException(e);
            }
        }
        log.info("initialized TAMConnector");
    }

    /**
     * Disposes of the {@link TAMConnector}'s resources.
     * 
     * @see Connector#dispose()
     */
    public void dispose() {
        final String method = "createPDContext";
        synchronized (TAMConnector.class) {
            count--;
            if (count == 0) {
                PDMessages msgs = new PDMessages();
                try {
                    log.info("Shutting down PDAdmin.");
                    PDAdmin.shutdown(msgs);
                    processMessages(method, msgs);
                }
                catch (PDException e) {
                    handlePDException(e);
                }
            }

        }
    }

    public void checkAlive() {
    }

    protected void createPDContext() {
        final String method = "createPDContext";
        log.info("Entry {0}", method);

        if (null == ctxt) {
            Locale locale = Locale.getDefault();
            if (locale == null) {
                // Create locale for US English
                locale = new Locale("ENGLISH", "US");
            }
            /*
            Create a PDContext object.
            The security context provides for the secure transfer of
            administrative requests and data between the Java application
            and the policy server.
             */
            try {
                URL configURL = new URL(configuration.getConfigurationFileURL());
                if (configuration.isCertificateBased()) {
                    if (log.isInfo()) {
                        log.info("new PDContext({0},{1})", locale.toString(), configURL.toString());
                    }
                    ctxt = new PDContext(locale, configURL);
                } else {
                    GuardedStringAccessor accessor = new GuardedStringAccessor();
                    configuration.getAdminPassword().access(accessor);
                    if (log.isInfo()) {
                        log.info("new PDContext({0},{1},adminPassword,{2})", locale.toString(), configuration.getAdminUserID(), configURL.toString());
                    }
                    /*
                    Create a security context using our locale. administrative
                    privileges in Access Manager its password and a URL of
                    the form file:/// by the SvrSslCfg class.
                     */
                    ctxt = new PDContext(locale,
                            configuration.getAdminUserID(),
                            accessor.getArray(),
                            configURL);

                }
                if (ctxt == null) {
                    throw new ConnectorIOException("TAM_START_PD_CONTEXT_ERROR");
                }
            }
            catch (MalformedURLException e) {
                throw new ConnectorException(e);
            }
            catch (PDException e) {
                convertPDExceptionToConnectorException(e);
            }
        }
        log.info("Exit {0}", method);
    }

    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objClass, final Set<Attribute> attributes, final OperationOptions options) {
        final String method = "create";
        log.info("Entry {0}", method);
        Uid uid = null;
        Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(attributes));
        Name name = AttributeUtil.getNameFromAttributes(attributes);
        log.info("create({0},{1})", objClass.getObjectClassValue(), name.getNameValue());

        String description = null;
        if (null != attrMap.get(PredefinedAttributes.DESCRIPTION)) {
            description = AttributeUtil.getStringValue(attrMap.get(PredefinedAttributes.DESCRIPTION));
        }

        boolean importFromRegistry = false;
        if (null != attrMap.get(ATTR_IMPORT_FROM_REGISTRY)) {
            importFromRegistry = AttributeUtil.getBooleanValue(attrMap.get(ATTR_IMPORT_FROM_REGISTRY));
        }

        String registryName = null;
        if (null != attrMap.get(ATTR_REGISTRY_NAME)) {
            registryName = AttributeUtil.getStringValue(attrMap.get(ATTR_REGISTRY_NAME));
        }

        if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
            /**------------------------------------------------------------------
             * Create a user, using the PDUser.createUser() static method, and
             * assign the user to a specific group. This method sends a
             * request to the policy server to create the user.
             *------------------------------------------------------------------ */
            // http://publib.boulder.ibm.com/infocenter/tivihelp/v2r1/index.jsp?topic=/com.ibm.itame.doc/com/tivoli/pd/jadmin/PDUser.html
            // http://publib.boulder.ibm.com/infocenter/tivihelp/v2r1/index.jsp?topic=/com.ibm.itame.doc/com/tivoli/pd/jadmin/PDGroup.html
            // Set up all of the user’s attributes
            String firstName = null;
            if (null != attrMap.get(ATTR_FIRST_NAME)) {
                firstName = AttributeUtil.getStringValue(attrMap.get(ATTR_FIRST_NAME));
            }
            String lastName = null;
            if (null != attrMap.get(ATTR_LAST_NAME)) {
                lastName = AttributeUtil.getStringValue(attrMap.get(ATTR_LAST_NAME));
            }

            GuardedString password = AttributeUtil.getPasswordValue(attributes);
            if (password == null) {
                throw new IllegalArgumentException("Missing Password value");
            }
            GuardedStringAccessor accessor = new GuardedStringAccessor();
            password.access(accessor);

            boolean ssoUser = false;
            if (null != attrMap.get(ATTR_SSO_USER)) {
                ssoUser = AttributeUtil.getBooleanValue(attrMap.get(ATTR_SSO_USER));
            }

            boolean pwdPolicy = true;
            if (null != attrMap.get(ATTR_PASSWORD_POLICY)) {
                pwdPolicy = AttributeUtil.getBooleanValue(attrMap.get(ATTR_PASSWORD_POLICY));
            }
            boolean isValid = true;
            Boolean expirePassword = null;
            if (null != attrMap.get(OperationalAttributes.ENABLE_NAME)) {
                expirePassword = AttributeUtil.getBooleanValue(attrMap.get(OperationalAttributes.ENABLE_NAME));
                isValid = expirePassword;
            }

            ArrayList groupList = new ArrayList();
            if (null != attrMap.get(PredefinedAttributes.GROUPS_NAME)) {
                groupList = new ArrayList(attrMap.get(PredefinedAttributes.GROUPS_NAME).getValue());
            }

            Attribute webCredList = null;
            if (null != attrMap.get(ATTR_GSO_WEB_CREDENTIALS)) {
                webCredList = attrMap.get(ATTR_GSO_WEB_CREDENTIALS);
            }
            Attribute groupCredList = null;
            if (null != attrMap.get(ATTR_GSO_GROUP_CREDENTIALS)) {
                groupCredList = attrMap.get(ATTR_GSO_GROUP_CREDENTIALS);
            }

            try {
                PDRgyUserName pdRgyUserName = new PDRgyUserName(registryName, firstName, lastName);
                PDMessages msgs = new PDMessages();

                if (importFromRegistry) {
                    if (log.isInfo()) {
                        log.info("PDUser.importUser(ctxt,{0},{1},null,{2},msgs)", name.getNameValue(),
                                pdRgyUserName,
                                ssoUser);
                    }
                    /*
                    public static void importUser(PDContext context,
                    java.lang.String pdName,
                    PDRgyUserName rgyName,
                    java.lang.String groupName,
                    boolean ssoUser,
                    PDMessages messages)
                    throws PDException
                    
                    Creates a user in the Policy Director Management Server by importing an existing user from the user registry.
                    
                    This constructor corresponds to the ivadmin_user_import() C API.
                    
                    Parameters:
                    context - the context for communicating with the Policy Director Management Server.
                    pdName - the Policy Director user name. This value may not be null and must have a non-zero length.
                    rgyName - an object specifying the registry user name. This value may not be null or specify a null or zero-length name.
                    groupName - the initial group to which the user belongs. Can be null.
                    ssoUser - true, if the user is capable of having single-signon credentials; false, otherwise.
                    messages - in/out parameter; empty PDMessages on input; may contain zero or more informational or warning messages on output.
                    Throws:
                    PDException - if an error occurs. This exception may contain error and message codes defined in the product Error Message Reference document.
                     */
                    PDUser.importUser(ctxt,
                            name.getNameValue(),
                            pdRgyUserName,
                            null,
                            ssoUser,
                            msgs);
                    uid = new Uid(name.getNameValue());
                    processMessages(method, msgs);
                    if (groupList != null) {
                        updateTAMGroups(groupList, name.getNameValue(), msgs);
                    }
                } else {
                    if (log.isInfo()) {
                        log.info("PDUser.createUser(ctxt,{0},{1},null,Passw0rd,{2},{3},{4},msgs)", name.getNameValue(), pdRgyUserName, groupList,
                                ssoUser, pwdPolicy);
                    }
                    /*
                    public static void createUser(PDContext context,
                    java.lang.String pdName,
                    PDRgyUserName rgyName,
                    java.lang.String description,
                    char[] pwd,
                    java.util.ArrayList groupNames,
                    boolean ssoUser,
                    boolean noPwdPolicy,
                    PDMessages messages)
                    throws PDException
                    
                    Creates a user in the Policy Director Management Server.
                    
                    This constructor corresponds to the ivadmin_user_create() C API.
                    
                    Parameters:
                    context - the context for communicating with the Policy Director Management Server.
                    pdName - the Policy Director user name. This value may not be null and must have a non-zero length.
                    rgyName - the registry user name. The registry name in this object must be non-null and have a non-zero length.
                    description - this argument is currently ignored; the description must be set explicitly using the setDescription method.
                    pwd - the user password. This password may not be null and must have a non-zero length.
                    groupNames - a list of group names (Strings) to which the user initially belongs. Can be null or empty.
                    ssoUser - true, if the user is capable of having single-signon credentials; false, otherwise.
                    noPwdPolicy - true, if password policy will not be enforced during creation; false, otherwise. This has no bearing on password policy enforcement after user creation.
                    messages - in/out parameter; empty PDMessages on input; may contain zero or more informational or warning messages on output.
                    Throws:
                    PDException - if an error occurs. This exception may contain error and message codes defined in the product Error Message Reference document.
                     *
                     * If description is nonnull then there is no exception but the msgs contains the message:
                     * Code=813,334,637 Text=HPDJA0109W   A nonnull value is being passed to an unsupported argument.
                     */
                    PDUser.createUser(ctxt, name.getNameValue(), pdRgyUserName, null, accessor.getArray(), groupList,
                            ssoUser, pwdPolicy, msgs);

                    uid = new Uid(name.getNameValue());
                    processMessages(method, msgs);
                    if (StringUtil.isNotBlank(description)) {
                        PDUser.setDescription(ctxt, name.getNameValue(), description, msgs);
                        processMessages(method, msgs);
                    }
                }
                // set account valid
                if (log.isInfo()) {
                    log.info("Calling PDUser.setAccountValid(ctxt,{0},true,msgs)", name.getNameValue());
                }
                PDUser.setAccountValid(ctxt, name.getNameValue(), true, msgs);
                processMessages(method, msgs);
                if (expirePassword != null) {
                    if (log.isInfo()) {
                        log.info("PDUser.setPasswordValid(ctxt,{0},{1},msgs)", name.getNameValue(), isValid);
                    }
                    PDUser.setPasswordValid(ctxt, name.getNameValue(), isValid, msgs);
                    processMessages(method, msgs);
                }
                if (webCredList != null) {
                    updateGSOCredentials(name.getNameValue(), PDSSOCred.PDSSOCRED_SSORESOURCE, webCredList, msgs);
                }
                if (groupCredList != null) {
                    updateGSOCredentials(name.getNameValue(), PDSSOCred.PDSSOCRED_SSORESOURCEGROUP, groupCredList, msgs);
                }
            }
            catch (PDException e) {
                convertPDExceptionToConnectorException(e);
            }
            catch (Exception e) {
                throw new ConnectorException(e);
            }
        } else if (objClass.is(ObjectClass.GROUP_NAME)) {
            try {
                PDMessages msgs = new PDMessages();
                PDRgyGroupName pdRgyGroupName = new PDRgyGroupName(registryName);
                if (importFromRegistry) {
                    if (log.isInfo()) {
                        log.info("PDGroup.importGroup(ctxt,{0},{1},{2},msgs", name.getNameValue(), pdRgyGroupName, description);
                    }
                    /*
                    public static void importGroup(PDContext context,
                    java.lang.String pdName,
                    PDRgyGroupName rgyName,
                    java.lang.String container,
                    PDMessages messages)
                    throws PDException
                    
                    Creates a group in the Policy Director Management Server by importing it from the group directory.
                    
                    This constructor corresponds to the ivadmin_group_import() C API.
                    
                    Parameters:
                    context - the context for communicating with the Policy Director Management Server.
                    pdName - the Policy Director group name.
                    rgyName - an object specifying the registry group name. This value may not be null or specify a null or zero-length name.
                    container - the container object within the management object space. This value can be null or zero-length to indicate the group is at the root level.
                    messages - in/out parameter; empty PDMessages on input; may contain zero or more informational or warning messages on output.
                    Throws:
                    PDException - if an error occurs. This exception may contain error and message codes defined in the product Error Message Reference document.
                     */
                    PDGroup.importGroup(ctxt, name.getNameValue(), pdRgyGroupName, description, msgs);
                    uid = new Uid(name.getNameValue());
                    processMessages(method, msgs);
                } else {
                    if (log.isInfo()) {
                        log.info("PDGroup.createGroup(ctxt,{0},{1},{2},null,msgs", name.getNameValue(), pdRgyGroupName, description);
                    }

                    /*
                    public static void createGroup(PDContext context,
                    java.lang.String pdName,
                    PDRgyGroupName rgyName,
                    java.lang.String description,
                    java.lang.String container,
                    PDMessages messages)
                    throws PDException
                    
                    Creates a group in the Policy Director Management Server.
                    
                    Parameters:
                    context - the context for communicating with the Policy Director Management Server.
                    pdName - the Policy Director group name. This value may not be null and must have a nonzero length.
                    rgyName - the registry group name. The registry name in this object must be nonnull and have a nonzero length.
                    description - this argument is currently ignored; the description must be set explicitly using the setDescription method.
                    container - the container object within the management object space. This value can be null or zero-length to indicate the group is at the root level.
                    messages - in/out parameter; empty PDMessages on input; may contain zero or more informational or warning messages on output.
                    Throws:
                    PDException - if an error occurs. This exception may contain error and message codes defined in the product Error Message Reference document.
                     *
                     * If description is nonnull then there is no exception but the msgs contains the message:
                     * Code=813,334,637 Text=HPDJA0109W   A nonnull value is being passed to an unsupported argument.
                     */
                    PDGroup.createGroup(ctxt, name.getNameValue(), pdRgyGroupName, null, null, msgs);
                    uid = new Uid(name.getNameValue());
                    processMessages(method, msgs);
                    if (StringUtil.isNotBlank(description)) {
                        PDGroup.setDescription(ctxt, name.getNameValue(), description, msgs);
                        processMessages(method, msgs);
                    }
                }
            }
            catch (PDException e) {
                convertPDExceptionToConnectorException(e);
            }
            catch (Exception e) {
                throw new ConnectorException(e);
            }
        } else {
            log.error("Failed to create: Unsupported objectClass={0}, id={1}", objClass, uid);
        }
        log.info("Exit {0}", method);
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public FilterTranslator<String> createFilterTranslator(ObjectClass objClass, OperationOptions options) {
        return new TAMFilterTranslator();
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objClass, String query, ResultsHandler handler, OperationOptions options) {
        final String method = "executeQuery";
        log.info("Entry {0}", method);
        Set<String> attributesToGet = null;
        if (options != null && options.getAttributesToGet() != null) {
            attributesToGet = CollectionUtil.newReadOnlySet(options.getAttributesToGet());
        } else {
            ObjectClassInfo oci = schema().findObjectClassInfo(objClass.getObjectClassValue());
            if (oci != null) {
                attributesToGet = new HashSet<String>();
                for (AttributeInfo a : oci.getAttributeInfo()) {
                    attributesToGet.add(a.getName());
                }
            } else {
                throw new ConnectorException("Object class " + objClass.getObjectClassValue() + "is not supported");
            }
        }


        if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
            try {
                if (StringUtil.isBlank(query)) {
                    query = PDUser.PDUSER_ALLPATTERN;
                }
                PDMessages msgs = new PDMessages();
                ArrayList userList = PDUser.listUsers(ctxt,
                        query,
                        PDUser.PDUSER_MAXRETURN,
                        false,
                        // don't match by registry
                        // names, match by Tivoli
                        // Access Manager names
                        msgs);
                if (log.isInfo()) {
                    log.info("PDUser.listUsers(ctxt,{0},{1},false,msgs)", query, PDUser.PDUSER_MAXRETURN);
                }
                if (userList == null || userList.isEmpty()) {
                    log.info("User Not Found for query={0}", query);
                    return;
                }

                if (getNamesOnly(attributesToGet)) {
                    for (Object o : userList) {
                        String name = (String) o;
                        ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                        bld.setUid(name);
                        bld.setName(name);
                        handler.handle(bld.build());
                    }
                } else {
                    for (Object o : userList) {
                        String name = (String) o;
                        ConnectorObjectBuilder bld = new ConnectorObjectBuilder();

                        PDUser pdUser = new PDUser(ctxt, name, msgs);
                        processMessages(method, msgs);
                        if (log.isInfo()) {
                            log.info("Process PDUser: {0} \n" + "RegistryName={1}\n" + "FirstName={2}\n" + "LastName={3}\n" + "Descr={4}\n" + "Groups={5}\n" + "SSOUser={6}\n", name, pdUser.getRgyName(), pdUser.getFirstName(), pdUser.getLastName(), pdUser.getDescription(), pdUser.getGroups(), pdUser.isSSOUser());
                        }

                        bld.setUid(pdUser.getId());
                        bld.setName(pdUser.getId());
                        bld.addAttribute(ATTR_REGISTRY_NAME, pdUser.getRgyName());

                        Map<String, List<String>> gsoCreds = null;

                        for (String attrName : attributesToGet) {

                            if (ATTR_FIRST_NAME.equalsIgnoreCase(attrName)) {
                                bld.addAttribute(ATTR_FIRST_NAME, pdUser.getFirstName());
                                continue;
                            }
                            if (ATTR_LAST_NAME.equalsIgnoreCase(attrName)) {
                                bld.addAttribute(ATTR_LAST_NAME, pdUser.getLastName());
                                continue;
                            }
                            if (PredefinedAttributes.DESCRIPTION.equalsIgnoreCase(attrName)) {
                                bld.addAttribute(PredefinedAttributes.DESCRIPTION, pdUser.getDescription());
                                continue;
                            }
                            if (PredefinedAttributes.GROUPS_NAME.equalsIgnoreCase(attrName)) {
                                bld.addAttribute(PredefinedAttributes.GROUPS_NAME, pdUser.getGroups());
                                continue;
                            }
                            if (ATTR_SSO_USER.equalsIgnoreCase(attrName)) {
                                bld.addAttribute(ATTR_SSO_USER, Boolean.valueOf(pdUser.isSSOUser()));
                                continue;
                            }
                            if (ATTR_GSO_WEB_CREDENTIALS.equalsIgnoreCase(attrName)) {
                                if (gsoCreds == null) {
                                    gsoCreds = getUserGSOCredentials(name, msgs);
                                }

                                bld.addAttribute(ATTR_GSO_WEB_CREDENTIALS, gsoCreds.get(PDSSOCred.PDSSOCRED_SSORESOURCE));
                                continue;
                            }
                            if (ATTR_GSO_GROUP_CREDENTIALS.equalsIgnoreCase(attrName)) {
                                if (gsoCreds == null) {
                                    gsoCreds = getUserGSOCredentials(name, msgs);
                                }
                                bld.addAttribute(ATTR_GSO_GROUP_CREDENTIALS, gsoCreds.get(PDSSOCred.PDSSOCRED_SSORESOURCEGROUP));
                                continue;
                            }
                            if (ATTR_EXPIRE_PASSWORD.equalsIgnoreCase(attrName)) {
                                bld.addAttribute(ATTR_EXPIRE_PASSWORD, pdUser.isPasswordValid());
                                continue;
                            }
                            if (OperationalAttributes.ENABLE_NAME.equalsIgnoreCase(attrName)) {
                                bld.addAttribute(OperationalAttributes.ENABLE_NAME, pdUser.isAccountValid());
                                continue;
                            }
                        }
                        // create the connector object..
                        handler.handle(bld.build());
                    }
                }
            }
            catch (PDException e) {
                convertPDExceptionToConnectorException(e);
            }
            catch (Exception e) {
                throw new ConnectorException(e);
            }
        } else if (objClass.is(ObjectClass.GROUP_NAME)) {
            try {
                if (StringUtil.isBlank(query)) {
                    query = PDGroup.PDGROUP_ALLPATTERN;
                }
                PDMessages msgs = new PDMessages();
                boolean listByRegistryName = false;
                ArrayList objectList = PDGroup.listGroups(ctxt, query, PDGroup.PDGROUP_MAXRETURN, listByRegistryName, msgs);

                if (getNamesOnly(attributesToGet)) {
                    for (Object o : objectList) {
                        String name = (String) o;
                        ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                        bld.setObjectClass(objClass);
                        bld.setUid(name);
                        bld.setName(name);
                        handler.handle(bld.build());
                    }
                } else {
                    for (Object o : objectList) {
                        String name = (String) o;
                        ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                        bld.setObjectClass(objClass);

                        PDGroup pdGroup = new PDGroup(ctxt, name, msgs);
                        bld.setUid(pdGroup.getId());
                        bld.setName(pdGroup.getId());
                        for (String attrName : attributesToGet) {
                            if (ATTR_REGISTRY_NAME.equalsIgnoreCase(attrName)) {
                                bld.addAttribute(ATTR_REGISTRY_NAME, pdGroup.getRgyName());
                                continue;
                            }
                            if (ATTR_GROUP_MEMBERS.equalsIgnoreCase(attrName)) {
                                bld.addAttribute(ATTR_GROUP_MEMBERS, pdGroup.getMembers());
                                continue;
                            }
                            if (PredefinedAttributes.DESCRIPTION.equalsIgnoreCase(attrName)) {
                                bld.addAttribute(PredefinedAttributes.DESCRIPTION, pdGroup.getDescription());
                                continue;
                            }
                        }
                        // create the connector object..
                        handler.handle(bld.build());
                    }
                }

            }
            catch (PDException e) {
                convertPDExceptionToConnectorException(e);
            }
            catch (Exception e) {
                throw new ConnectorException(e);
            }
        } else if (objClass.is(TYPE_GSO_RESOURCE)) {
            try {
                PDMessages msgs = new PDMessages();
                ArrayList objectList = PDSSOResource.listSSOResources(ctxt, msgs);
                for (Object o : objectList) {
                    ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                    bld.setUid((String) o);
                    bld.setName((String) o);
                    handler.handle(bld.build());
                }
            }
            catch (PDException e) {
                convertPDExceptionToConnectorException(e);
            }
            catch (Exception e) {
                throw new ConnectorException(e);
            }
        } else if (objClass.is(TYPE_GSO_GROUP_RESOURCE)) {
            try {
                PDMessages msgs = new PDMessages();
                ArrayList objectList = PDSSOResourceGroup.listSSOResourceGroups(ctxt, msgs);
                for (Object o : objectList) {
                    ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                    bld.setUid((String) o);
                    bld.setName((String) o);
                    handler.handle(bld.build());
                }
            }
            catch (PDException e) {
                convertPDExceptionToConnectorException(e);
            }
            catch (Exception e) {
                throw new ConnectorException(e);
            }
        }
        log.info("Exit {0}", method);
    }

    /**
     * Help to optimize the operation if the list method is enought.
     * 
     * @param attributesToGet list of attributes to return.
     * @return true if the {@code attributesToGet} contains only the __NAME__ and __UID__
     */
    private boolean getNamesOnly(Set<String> attributesToGet) {
        if (null != attributesToGet && !attributesToGet.isEmpty() && attributesToGet.size() < 3) {
            for (String attr : attributesToGet) {
                if (!Name.NAME.equals(attr) && !Uid.NAME.equals(attr)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void test() {
    }

    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objClass,
            Uid uid,
            Set<Attribute> replaceAttributes,
            OperationOptions options) {
        final String method = "update";
        log.info("Entry {0}", method);
        if (ObjectClass.ACCOUNT.is(objClass.getObjectClassValue())) {
            try {
                PDMessages msgs = new PDMessages();
                GuardedString password = AttributeUtil.getPasswordValue(replaceAttributes);
                if (null != password) {
                    GuardedStringAccessor accessor = new GuardedStringAccessor();
                    password.access(accessor);
                    if (log.isInfo()) {
                        log.info("PDUser.setPassword(ctxt,{0},Password,msgs)", uid.getUidValue());
                    }
                    PDUser.setPassword(ctxt, uid.getUidValue(), accessor.getArray(), msgs);
                    processMessages(method, msgs);

                    if (configuration.isSyncGSOCredentials()) {
                        updateGSOCredentialPasswords(uid.getUidValue(), accessor);
                    }
                }

                Attribute firstNameAttr = AttributeUtil.find(ATTR_FIRST_NAME, replaceAttributes);
                if (null != firstNameAttr) {
                    if (log.isInfo()) {
                        log.info("PDAdmin API does not support the firstName update.");
                    }
                }

                Attribute lastNameAttr = AttributeUtil.find(ATTR_LAST_NAME, replaceAttributes);
                if (null != lastNameAttr) {
                    if (log.isInfo()) {
                        log.info("PDAdmin API does not support the lastName update.");
                    }
                }

                Attribute descriptionAttr = AttributeUtil.find(PredefinedAttributes.DESCRIPTION, replaceAttributes);
                if (null != descriptionAttr) {
                    String description = AttributeUtil.getStringValue(descriptionAttr);
                    if (StringUtil.isNotBlank(description)) {
                        if (log.isInfo()) {
                            log.info("PDUser.setDescription(ctxt,{0},{1},msgs)", uid.getUidValue(), description);
                        }
                        PDUser.setDescription(ctxt, uid.getUidValue(), description, msgs);
                        processMessages(method, msgs);
                    }
                }

                Attribute enableAttr = AttributeUtil.find(OperationalAttributes.ENABLE_NAME, replaceAttributes);
                if (null != enableAttr) {
                    Boolean enable = AttributeUtil.getBooleanValue(enableAttr);
                    if (log.isInfo()) {
                        log.info("PDUser.setPasswordValid(ctxt,{0},{1},msgs)", uid.getUidValue(), enable);
                    }
                    PDUser.setPasswordValid(ctxt, uid.getUidValue(), enable, msgs);
                    processMessages(method, msgs);
                }

                Attribute ssoUserAttr = AttributeUtil.find(ATTR_SSO_USER, replaceAttributes);
                if (null != ssoUserAttr) {
                    Boolean ssoUser = AttributeUtil.getBooleanValue(ssoUserAttr);
                    if (log.isInfo()) {
                        log.info("PDUser.setSSOUser(ctxt,{0},{1},msgs)", uid.getUidValue(), ssoUser);
                    }
                    PDUser.setSSOUser(ctxt, uid.getUidValue(), ssoUser, msgs);
                    processMessages(method, msgs);
                }

                Attribute groupsAttr = AttributeUtil.find(PredefinedAttributes.GROUPS_NAME, replaceAttributes);
                if (null != groupsAttr) {
                    updateTAMGroups(groupsAttr.getValue(), uid.getUidValue(), msgs);
                }
                Attribute gsoWebCredentialsAttr = AttributeUtil.find(ATTR_GSO_WEB_CREDENTIALS, replaceAttributes);
                if (null != gsoWebCredentialsAttr) {
                    updateGSOCredentials(uid.getUidValue(), PDSSOCred.PDSSOCRED_SSORESOURCE, gsoWebCredentialsAttr, msgs);
                }
                Attribute gsoGroupCredentialsAttr = AttributeUtil.find(ATTR_GSO_WEB_CREDENTIALS, replaceAttributes);
                if (null != gsoGroupCredentialsAttr) {
                    updateGSOCredentials(uid.getUidValue(), PDSSOCred.PDSSOCRED_SSORESOURCEGROUP, gsoGroupCredentialsAttr, msgs);
                }


            }
            catch (PDException e) {
                convertPDExceptionToConnectorException(e);
            }
            catch (Exception e) {
                throw new ConnectorException(e);
            }

        } else if (ObjectClass.GROUP.is(objClass.getObjectClassValue())) {
        } else {
            log.error("Failed to update: Unsupported objectClass={0}, id={1}", objClass, uid);
        }
        log.info("Exit {0}", method);
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objClass, final Uid uid, final OperationOptions options) {
        final String method = "delete";
        log.info("Entry {0}", method);
        boolean deleteFromRegistry = configuration.isDeleteFromRegistry();
        if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
            /* 
            To delete an object, use the static deletion method associated
            with the administration object. For example, to delete an
            Tivoli Access Manager user, you would use the PDUser.deleteUser()
            static method
             */
            try {
                PDMessages msgs = new PDMessages();
                log.info("PDUser.deleteUser(ctxt,{0},{1},msgs)", uid.getUidValue(), deleteFromRegistry);
                PDUser.deleteUser(ctxt, uid.getUidValue(), deleteFromRegistry, msgs);
                processMessages(method, msgs);
            }
            catch (PDException e) {
                convertPDExceptionToConnectorException(e);
            }
            catch (Exception e) {
                throw new ConnectorException(e);
            }
        } else if (objClass.is(ObjectClass.GROUP_NAME)) {
            try {
                PDMessages msgs = new PDMessages();
                log.info("PDGroup.deleteGroup(ctxt,{0},{1},msgs)", uid.getUidValue(), deleteFromRegistry);
                PDGroup.deleteGroup(ctxt, uid.getUidValue(), deleteFromRegistry, msgs);
                processMessages(method, msgs);
            }
            catch (PDException e) {
                convertPDExceptionToConnectorException(e);
            }
            catch (Exception e) {
                throw new ConnectorException(e);
            }
        } else {
            log.error("Failed to delete: Unsupported objectClass={0}, id={1}", objClass, uid);
        }
        log.info("Exit {0}", method);
    }

    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        if (null == schema) {
            SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());

            // GSO WEB Resource
            ObjectClassInfoBuilder ocBuilder = new ObjectClassInfoBuilder();
            ocBuilder.setType(TYPE_GSO_RESOURCE);
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.REQUIRED)));
            ObjectClassInfo objectClassInfo = ocBuilder.build();
            schemaBuilder.defineObjectClass(objectClassInfo);
            schemaBuilder.removeSupportedObjectClass(CreateOp.class, objectClassInfo);
            schemaBuilder.removeSupportedObjectClass(UpdateOp.class, objectClassInfo);
            schemaBuilder.removeSupportedObjectClass(DeleteOp.class, objectClassInfo);


            // GSO GROUP Resource
            ocBuilder = new ObjectClassInfoBuilder();
            ocBuilder.setType(TYPE_GSO_GROUP_RESOURCE);
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.REQUIRED)));
            objectClassInfo = ocBuilder.build();
            schemaBuilder.defineObjectClass(objectClassInfo);
            schemaBuilder.removeSupportedObjectClass(CreateOp.class, objectClassInfo);
            schemaBuilder.removeSupportedObjectClass(UpdateOp.class, objectClassInfo);
            schemaBuilder.removeSupportedObjectClass(DeleteOp.class, objectClassInfo);


            // Group
            ocBuilder = new ObjectClassInfoBuilder();
            ocBuilder.setType(ObjectClass.GROUP_NAME);
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.REQUIRED)));
            //ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(Uid.NAME, String.class, EnumSet.of(Flags.REQUIRED)));
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_REGISTRY_NAME, String.class, EnumSet.of(Flags.REQUIRED)));
            ocBuilder.addAttributeInfo(PredefinedAttributeInfos.DESCRIPTION);
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_GROUP_MEMBERS, String.class, EnumSet.of(Flags.MULTIVALUED)));
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_IMPORT_FROM_REGISTRY, Boolean.class));
            schemaBuilder.defineObjectClass(ocBuilder.build());


            // Users
            ocBuilder = new ObjectClassInfoBuilder();
            ocBuilder.setType(ObjectClass.ACCOUNT_NAME);
            //The name of the object
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.REQUIRED, Flags.NOT_UPDATEABLE)));
            //User registry name
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_REGISTRY_NAME, String.class, EnumSet.of(Flags.REQUIRED, Flags.NOT_UPDATEABLE)));
            ocBuilder.addAttributeInfo(PredefinedAttributeInfos.DESCRIPTION);
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_FIRST_NAME, String.class, EnumSet.of(Flags.NOT_UPDATEABLE)));
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_LAST_NAME, String.class, EnumSet.of(Flags.NOT_UPDATEABLE)));
            ocBuilder.addAttributeInfo(PredefinedAttributeInfos.LAST_LOGIN_DATE);
            ocBuilder.addAttributeInfo(PredefinedAttributeInfos.GROUPS);
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_SSO_USER, Boolean.class));
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_IMPORT_FROM_REGISTRY, Boolean.class));
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_EXPIRE_PASSWORD, Boolean.class));
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_PASSWORD_POLICY, Boolean.class, EnumSet.of(Flags.NOT_UPDATEABLE)));
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_GSO_GROUP_CREDENTIALS, String.class, EnumSet.of(Flags.MULTIVALUED)));
            ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_GSO_WEB_CREDENTIALS, String.class, EnumSet.of(Flags.MULTIVALUED)));
            ocBuilder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);
            ocBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
            schemaBuilder.defineObjectClass(ocBuilder.build());
            schema = schemaBuilder.build();
        }
        return schema;
    }

    public Uid authenticate(ObjectClass oc, String user, GuardedString gs, OperationOptions oo) {
        final String method = "authenticate";
        log.info("Entry {0}", method);
        Uid uid = null;
        if (ObjectClass.ACCOUNT.is(oc.getObjectClassValue())) {
            try {
                PDMessages msgs = new PDMessages();
                PDUser pdUser = new PDUser(ctxt, user, msgs);
                processMessages(method, msgs);
                /* NOTICE!!!
                 * ENABLED checks the isAccountValid() and here we
                 * check the isPasswordValid()
                 */
                if (pdUser.isPasswordValid()) {
                    uid = new Uid(user);
                }
            }
            catch (PDException e) {
                convertPDExceptionToConnectorException(e);
            }
            catch (Exception e) {
                throw new ConnectorException(e);
            }
        }
        log.info("Exit {0}", method);
        return uid;
    }

    static void processMessages(String method, PDMessages msgs) {
        log.info("Start PDMessages in {0}", method);
        if (msgs != null) {
            Iterator i = msgs.listIterator();
            while (i.hasNext()) {
                PDMessage msg = (PDMessage) i.next();
                log.info("Tivoli Access Manager Message: Code={0} Text={1}", msg.getMsgCode(), msg.getMsgText());
            }
            msgs.clear();
        }
        log.info("End PDMessages in {0}", method);
    }

    static void handlePDException(Exception e) {
        PDException pd = (PDException) e;
        PDMessages msgs = pd.getMessages();
        if (msgs != null) {
            Iterator pdi = msgs.iterator();
            PDMessage msg = null;
            int msgCode = 0;

            /*---------------------------------------------------------------
             * The Tivoli Access Manager Java Admin API will throw PDExceptions
             * that have a single message code in the member PDMessages.
             * However, the PDException class is designed so that multiple codes
             * can be returned.  This way, a caller of the Java Admin API can
             * "stack" another error code in the PDException, if desired,
             * and rethrow the exception to its caller.
             *---------------------------------------------------------------
             */

            while (pdi.hasNext()) {
                msg = (PDMessage) pdi.next();
                msgCode = msg.getMsgCode();
                /*---------------------------------------------------------------
                 * Here are examples of generic Tivoli Access Manager message codes.
                 * They are available by importing one or more of the
                 * com.tivoli.pd.nls.pd*msg classes. These message codes are
                 * documented in the Error Message Reference.
                 *---------------------------------------------------------------
                 */
                switch (msgCode) {
                    case 348132396:
                        log.error("***  An invalid group identification or Distinguished Name (DN) was specified. ***");
                        log.error("Message text is: " + msg.getMsgText() + "\n");
                        break;
                    case 348132088:
                        log.error("***  The group name already exists in the registry ***");
                        log.error("Message text is: " + msg.getMsgText() + "\n");
                        break;
                    case pdbjamsg.bja_invalid_msgs:
                        log.error("*** Invalid PDMessage Parameter ***");
                        log.error("Message text is: " + msg.getMsgText() + "\n");
                        break;
                    case pdbjamsg.bja_invalid_ctxt:
                        log.error("*** Invalid Context Parameter ***");
                        log.error("Message text is: " + msg.getMsgText() + "\n");
                        break;
                    case pdbjamsg.bja_cannot_contact_server:
                        log.error("*** The Server cannot be contacted ***");
                        log.error("Message text is: " + msg.getMsgText() + "\n");
                        break;
                    default:
                        log.error("Message text is: " + msg.getMsgText() + "\n");
                        break;
                }
            }
        } else {
            /*---------------------------------------------------------------
             * A PDException with no messages typically means that a Java
             * exception or error was thrown and wrappered in a PDException.
             * To get the underlying exception or error, use the PDException
             * getCause() method.
             *---------------------------------------------------------------
             */

            Throwable t = pd.getCause();
            if (t != null) {
                log.error(t, "*** Underlying Java Exception ***");
                t.printStackTrace();
            }
        }
    }

    static void convertPDExceptionToConnectorException(PDException pd) throws ConnectorException {
        final String method = "convertPDExceptionToConnectorException";
        log.info("Entry {0}", method);
        PDMessages msgs = pd.getMessages();
        if (msgs != null) {
            Iterator pdi = msgs.iterator();
            ConnectorException e = null;
            while (pdi.hasNext()) {
                PDMessage msg = (PDMessage) pdi.next();
                int msgCode = msg.getMsgCode();
                if (log.isError()) {
                    log.error("Access Manager Exception: Code={0}, Severity={1}, Text={2}", msg.getMsgCode(), msg.getMsgSeverity(), msg.getMsgText());
                }
                if (e == null) {
                    if (msgCode == 348132087) {
                        e = new AlreadyExistsException(msg.getMsgText(), pd);
                    } else {
                        e = new ConnectorIOException(convertPDMessage(msg), pd);
                    }
                    // code: 813334644 = "HPDJA0116E   Cannot contact server."
                    // code: 320938184 = "HPDIA0200W   Authentication failed. You have used an invalid user name, password or client certificate."
                    // code: 348132087 = "HPDMG0759W   The user name already exists in the registry."
                    // code: 348132117 = "HPDMG0789W   The user Distinguished Name (DN) cannot be created because it already exists."
                    // code: 348132089 = "HPDMG0761W   The entry referred to by the Distinguished Name (DN) must be a person entry."
                    // code: 348132082 = "HPDMG0754W   The entry was not found. If a user or group is being created, ensure that the Distinguished Name (DN) specified has the correct syntax and is valid."
                    // code: 348132396 = "HPDMG1068E   An invalid group identification or Distinguished Name (DN) was specified."
                    // code: 348132090 = ""
                }
            }
            throw e;
        } else {
            Throwable t = pd.getCause();
            if (t != null) {
                throw new ConnectorIOException("Access Manager Exception:", t);
            }
        }
        log.info("Exit {0}", method);
    }

    private static String convertPDMessage(PDMessage msg) {
        StringBuilder sb = new StringBuilder("Access Manager Exception ");
        if (PDMessage.PDMESSAGE_SEVERITY_INFO == msg.getMsgSeverity()) {
            sb.append("[INFO]");
        } else if (PDMessage.PDMESSAGE_SEVERITY_WARNING == msg.getMsgSeverity()) {
            sb.append("[WARNING]");
        } else if (PDMessage.PDMESSAGE_SEVERITY_ERROR == msg.getMsgSeverity()) {
            sb.append("[ERROR]");
        } else {
            sb.append("[OTHER]");
        }
        sb.append(" [").append(msg.getMsgCode()).append("] - ").append(msg.getMsgText());
        if (null != msg.getMsgArgs()) {
            sb.append(" args{");
            for (int i = 0; i < msg.getMsgArgs().length; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(i).append(":").append(msg.getMsgArgs()[i]);
            }
            sb.append("}");
        }
        return sb.toString();
    }

    protected void updateGSOCredentialPasswords(String uid, GuardedStringAccessor password) {
        final String method = "updateGSOCredentialPasswords";
        log.info("Entry {0}", method);
        try {
            PDMessages msgs = new PDMessages();
            ArrayList creds = PDSSOCred.listAndShowSSOCreds(ctxt, uid, msgs);
            processMessages(method, msgs);
            if (null != creds) {
                for (Object o : creds) {
                    PDSSOCred.CredInfo credInfo = (PDSSOCred.CredInfo) o;
                    String resName = credInfo.getResourceName();
                    String resType = credInfo.getResourceType();
                    String resUser = credInfo.getResourceUser();
                    if (log.isInfo()) {
                        log.info("PDSSOCred.setSSOCred(ctxt,{0},{1},{2},{3},Password,msgs)", uid, resName, resType,
                                resUser);
                    }
                    PDSSOCred.setSSOCred(ctxt, uid, resName, resType, resUser, password.getArray(), msgs);
                    processMessages(method, msgs);
                }
            }
        }
        catch (PDException e) {
            convertPDExceptionToConnectorException(e);
        }
        catch (Exception e) {
            throw new ConnectorException(e);
        }
        log.info("Exit {0}", method);
    }

    protected void updateGSOCredentials(String uid, String type, Attribute credentials, PDMessages msgs) throws PDException {
        final String method = "updateGSOCredentials";
        log.info("Entry {0}", method);

        //TODO: Fix the casting
        List<String> newCredentials = new ArrayList(credentials.getValue());

        // get Users Current GSO Credentials
        Map<String, List<String>> credInfo = getUserGSOCredentials(uid, msgs);
        List<String> oldCredentials = (ArrayList) credInfo.get(type);
        List<String> newCredentialKeys = getCredentialKeys(newCredentials);
        List<String> oldCredentialKeys = getCredentialKeys(oldCredentials);

        if (log.isInfo()) {
            log.info("Credential type={0}\n" + "Old Credentials={1}\n" + "New Credentials={2}\n" + "Old Credential keys={3}\n" + "New Credential keys={4}", type, oldCredentials, newCredentials, oldCredentialKeys, newCredentialKeys);

        }
        //REMOVE
        if (oldCredentialKeys != null) {
            if (!oldCredentialKeys.isEmpty()) {
                for (String cred : oldCredentials) {
                    if (!newCredentialKeys.contains(getCredentialKey(cred))) {
                        // delete credential from user
                        if (log.isInfo()) {
                            log.info("PDSSOCredential.deleteSSOCredential(ctxt,{0},{1},{2},msgs)", getCredentialKey(cred), type, uid);
                        }
                        PDSSOCred.deleteSSOCred(ctxt, getCredentialKey(cred), type, uid, msgs);
                        processMessages(method, msgs);
                    }
                }
            }
        }
        //ADD
        if (newCredentials != null) {
            if (!newCredentials.isEmpty()) {
                List<String> creds = new ArrayList<String>(newCredentials);
                for (String cred : creds) {
                    StringTokenizer tok = new StringTokenizer(cred, TOKEN_GSO_RESOURCE, false);
                    String credKey = null;
                    if (tok != null) {
                        credKey = tok.nextToken();
                    }
                    if (!cred.equalsIgnoreCase("") && !oldCredentialKeys.contains(credKey)) {
                        addGSOCredential(uid, type, cred, msgs);
                        newCredentials.remove(cred);
                    }
                }
            }
        }
        //UPDATE
        if (newCredentials != null) {
            if (!newCredentials.isEmpty()) {
                for (String cred : newCredentials) {
                    if (!cred.equalsIgnoreCase("")) {
                        updateGSOCredential(uid, type, cred, msgs);
                    }
                }
            }
        }
        log.info("Exit {0}", method);
    }

    protected List<String> getCredentialKeys(List<String> creds) {
        final String method = "getCredentialKeys";
        log.info("Entry {0}", method);
        List<String> credentialKeys = new ArrayList<String>();
        if (creds != null) {
            for (String strCredential : creds) {
                StringTokenizer tok = new StringTokenizer(strCredential, TOKEN_GSO_RESOURCE, false);
                if (tok != null && tok.countTokens() > 1) {
                    credentialKeys.add(tok.nextToken());
                }
            }
        }
        log.info("Exit {0}", method);
        return credentialKeys;
    }

    protected String getCredentialKey(String credential) {
        final String method = "getCredentialKey";
        log.info("Entry {0}", method);
        String credentialKey = null;
        if (credential != null) {
            StringTokenizer tok = new StringTokenizer(credential, TOKEN_GSO_RESOURCE, false);
            if (tok != null && tok.countTokens() > 1) {
                credentialKey = tok.nextToken();
            }
        }
        log.info("Exit {0}", method);
        return credentialKey;
    }

    protected void addGSOCredential(String uid, String type, String cred, PDMessages msgs) throws PDException {
        final String method = "addGSOCredential";
        log.info("Entry {0}", method);
        String resName = null;
        String resUser = null;
        String resPassword = null;
        StringTokenizer tok = new StringTokenizer(cred, TOKEN_GSO_RESOURCE, false);
        if (tok != null && tok.countTokens() > 1) {
            resName = tok.nextToken();
            resUser = tok.nextToken();
            if (tok.hasMoreTokens()) {
                resPassword = tok.nextToken();
            }
        }

        if (resPassword != null) {
            GuardedStringAccessor accessor = new GuardedStringAccessor();
            GuardedString password = new GuardedString(resPassword.toCharArray());
            password.access(accessor);
            if (log.isInfo()) {
                log.info("PDSSOCredential.createSSOCredential(ctxt,{0},{1},{2},{3},Password,msgs)", resName, type, uid, resUser);
            }
            PDSSOCred.createSSOCred(ctxt, resName, type, uid, resUser, accessor.getArray(), msgs);
            processMessages(method, msgs);
        } else {
            //TODO: FIX ME
        }
        log.info("Exit {0}", method);
    }

    protected void updateGSOCredential(String uid, String type, String cred, PDMessages msgs) throws PDException {
        final String method = "updateGSOCredential";
        log.info("Entry {0}", method);
        String resName = null;
        String resUser = null;
        String resPassword = null;
        StringTokenizer tok = new StringTokenizer(cred, TOKEN_GSO_RESOURCE, false);
        if (tok != null && tok.countTokens() > 1) {
            resName = tok.nextToken();
            resUser = tok.nextToken();
            if (tok.hasMoreTokens()) {
                resPassword = tok.nextToken();
            }
        }
        GuardedStringAccessor accessor = new GuardedStringAccessor();
        if (resPassword != null) {
            GuardedString password = new GuardedString(resPassword.toCharArray());
            password.access(accessor);
        }
        if (log.isInfo()) {
            log.info("PDSSOCredential.setSSOCredential(ctxt,{0},{1},{2},{3},Password,msgs)", uid, resName, type, resUser);
        }
        PDSSOCred.setSSOCred(ctxt, uid, resName, type, resUser, resPassword != null ? accessor.getArray() : PDSSOCred.PDSSOCRED_EMPTYPASSWORD, msgs);
        processMessages(method, msgs);
        log.info("Exit {0}", method);
    }

    protected Map<String, List<String>> getUserGSOCredentials(String uid, PDMessages msgs)
            throws PDException {
        final String method = "getUserGSOCredentials";
        log.info("Entry {0}", method);
        ArrayList gsoCredentialInfo = PDSSOCred.listAndShowSSOCreds(ctxt, uid, msgs);
        processMessages(method, msgs);
        List<String> gsoWebCredentials = new ArrayList<String>();
        List<String> gsoGroupCredentials = new ArrayList<String>();
        if (gsoCredentialInfo != null) {
            for (Object o : gsoCredentialInfo) {
                PDSSOCred.CredInfo credInfo = (PDSSOCred.CredInfo) o;
                StringBuilder sb = new StringBuilder(credInfo.getResourceName());
                sb.append(TOKEN_GSO_RESOURCE);
                sb.append(credInfo.getResourceUser());
                if (credInfo.getResourceType().equalsIgnoreCase(PDSSOCred.PDSSOCRED_SSORESOURCEGROUP)) {
                    gsoGroupCredentials.add(sb.toString());
                } else {
                    gsoWebCredentials.add(sb.toString());
                }
            }
        }
        if (log.isInfo()) {
            log.info("gsoWebCredentials = ", gsoWebCredentials);
            log.info("gsoGroupCredentials = ", gsoGroupCredentials);
        }
        Map<String, List<String>> credInfo = new HashMap<String, List<String>>(2);
        credInfo.put(PDSSOCred.PDSSOCRED_SSORESOURCE, gsoWebCredentials);
        credInfo.put(PDSSOCred.PDSSOCRED_SSORESOURCEGROUP, gsoGroupCredentials);
        log.info("Exit {0}", method);
        return credInfo;
    }

    protected void updateTAMGroups(List<Object> groupsAttribute, String uid, PDMessages msgs) throws PDException {
        final String method = "updateTAMGroups";
        log.info("Entry {0}", method);
        // The groupsAttribute is an UnmodifiableRandomAccessList
        List<String> newGroups = new ArrayList<String>(groupsAttribute.size());
        for (Object group : groupsAttribute) {
            if (group instanceof String) {
                newGroups.add((String) group);
            }
        }
        PDUser pdUser = new PDUser(ctxt, uid, msgs);
        processMessages(method, msgs);
        ArrayList oldGroups = pdUser.getGroups();
        if (log.isInfo()) {
            log.info("Current Groups: {0}", oldGroups);
        }
        ArrayList users = new ArrayList();
        users.add(uid);
        if (!oldGroups.isEmpty()) {
            for (String group : (List<String>) oldGroups) {
                if (newGroups.contains(group)) {
                    newGroups.remove(group);
                } else {
                    PDGroup.removeMembers(ctxt, group, users, msgs);
                    processMessages(method, msgs);
                }
            }
        }
        if (!newGroups.isEmpty()) {
            for (String group : newGroups) {
                PDGroup.addMembers(ctxt, group, users, msgs);
                processMessages(method, msgs);
            }
        }
        log.info("Exit {0}", method);
    }
}
