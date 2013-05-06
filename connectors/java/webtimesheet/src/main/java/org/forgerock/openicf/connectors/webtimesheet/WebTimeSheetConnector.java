/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openicf.connectors.webtimesheet;

import java.util.*;
import org.identityconnectors.common.security.*;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.common.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main implementation of the WebTimeSheet Connector
 *
 * @author Robert Jackson - <a href='http://www.nulli.com'>Nulli</a>
 */
@ConnectorClass(
        displayNameKey = "WebTimeSheet",
configurationClass = WebTimeSheetConfiguration.class)
public class WebTimeSheetConnector implements PoolableConnector, CreateOp, UpdateOp, DeleteOp, SchemaOp, SearchOp<String>, TestOp {

    public static final java.lang.String ATTR_LAST_NAME = "LastName";
    public static final java.lang.String ATTR_FIRST_NAME = "FirstName";
    public static final java.lang.String ATTR_LOGIN_NAME = "LoginName";
    public static final java.lang.String ATTR_ID = "Id";
    public static final java.lang.String ATTR_EMPLOYEE_ID = "EmployeeId";
    public static final java.lang.String ATTR_INTERNAL_EMAIL = "Email";
    public static final java.lang.String ATTR_PARENT_ID = "ParentDepartmentId";
    public static final java.lang.String ATTR_DEPARTMENT = "DepartmentId";
    public static final java.lang.String ATTR_DOMAIN = "Domain";
    public static final java.lang.String ATTR_AUTH_TYPE = "AuthenticationType";
    public static final java.lang.String OBCLASS_DEPARTMENT_NAME = "Department";
    /**
     * Setup logging for the {@link WebTimeSheetConnector}.
     */
    private static final Log log = Log.getLog(WebTimeSheetConnector.class);
    /**
     * Place holder for the Connection created in the init method
     */
    private RepliConnectClient connection;
    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link WebTimeSheetConnector#init}.
     */
    private WebTimeSheetConfiguration config;

    /**
     * Gets the Configuration context for this connector.
     */
    public Configuration getConfiguration() {
        return this.config;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     *
     * @see Connector#init
     */
    public void init(Configuration cfg) {
        this.config = (WebTimeSheetConfiguration) cfg;
        this.connection = new RepliConnectClient(this.config);
    }

    /**
     * Disposes of the {@link WebTimeSheetConnector}'s resources.
     *
     * @see Connector#dispose()
     */
    public void dispose() {
        config = null;
        if (connection != null) {
            connection.dispose();
            connection = null;
        }
    }

    public void checkAlive() {
        connection.testConnection();
    }

    /**
     * ****************
     * SPI Operations
     *
     * Implement the following operations using the contract and description
     * found in the Javadoc for these methods.
     *****************
     */
    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objClass, final Set<Attribute> attrs, final OperationOptions options) {
        if (ObjectClass.ACCOUNT.equals(objClass)) {
            
            return connection.createUser(attrs, "1");

        } else if (objClass.is(OBCLASS_DEPARTMENT_NAME)) {
            throw new IllegalArgumentException("Creation of Departments not yet implemented");
        } else {
            throw new IllegalArgumentException("Unsupported objectclass '" + objClass + "'");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objClass, final Uid uid, final OperationOptions options) {
        if (ObjectClass.ACCOUNT.equals(objClass)) {
                    connection.deleteUser(uid.getUidValue());
        } else if (objClass.is(OBCLASS_DEPARTMENT_NAME)) {
            throw new IllegalArgumentException("Deletions of Departments not yet implemented");
        } else {
            throw new IllegalArgumentException("Unsupported objectclass '" + objClass + "'");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());

        Set<AttributeInfo> attributes = new HashSet<AttributeInfo>();

        //User Objects

        // Required Attributes
        //
        AttributeInfoBuilder aib = new AttributeInfoBuilder();
        //aib.setCreateable(true);
        aib.setUpdateable(false);
        aib.setName(Name.NAME);
        attributes.add(aib.build());

        aib.setName(ATTR_ID);
        attributes.add(aib.build());


        // regular attributes
        //
        attributes.add(AttributeInfoBuilder.build(ATTR_LAST_NAME));
        attributes.add(AttributeInfoBuilder.build(ATTR_FIRST_NAME));
        attributes.add(AttributeInfoBuilder.build(ATTR_LOGIN_NAME));
        attributes.add(AttributeInfoBuilder.build(ATTR_DEPARTMENT));
        attributes.add(AttributeInfoBuilder.build(ATTR_EMPLOYEE_ID));
        attributes.add(AttributeInfoBuilder.build(ATTR_INTERNAL_EMAIL));
        attributes.add(AttributeInfoBuilder.build(ATTR_DOMAIN));
        attributes.add(AttributeInfoBuilder.build(ATTR_AUTH_TYPE));

        // Operational attributes
        //
        attributes.add(OperationalAttributeInfos.PASSWORD);
        attributes.add(OperationalAttributeInfos.ENABLE);

        schemaBuilder.defineObjectClass(ObjectClass.ACCOUNT_NAME, attributes);


        // Department objects
        //
        ObjectClassInfoBuilder oib = new ObjectClassInfoBuilder();
        oib.setContainer(true);
        oib.setType(OBCLASS_DEPARTMENT_NAME);

        oib.addAttributeInfo(new AttributeInfoBuilder().setUpdateable(false).setName(Name.NAME).build());
        oib.addAttributeInfo(new AttributeInfoBuilder().setUpdateable(false).setName(ATTR_ID).build());
        oib.addAttributeInfo(new AttributeInfoBuilder().setUpdateable(false).setName(ATTR_PARENT_ID).build());

        final ObjectClassInfo department = oib.build();

        schemaBuilder.defineObjectClass(department);
        schemaBuilder.removeSupportedObjectClass(CreateOp.class, department);
        schemaBuilder.removeSupportedObjectClass(DeleteOp.class, department);
        schemaBuilder.removeSupportedObjectClass(SearchOp.class, department);
        schemaBuilder.removeSupportedObjectClass(UpdateOp.class, department);


        return schemaBuilder.build();
    }

    public FilterTranslator<String> createFilterTranslator(ObjectClass objClass, OperationOptions options) {
        return new WebTimeSheetFilterTranslator();
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objClass, String query, ResultsHandler handler, OperationOptions options) {
        if (ObjectClass.ACCOUNT.equals(objClass)) {
            JSONObject response;
            if (query == null) {
                response = connection.listUsers(query);
            } else {
                response = connection.getUser(query);
            }
            
            try {
                JSONArray users = response.getJSONArray("Value");
                for (int a = 0; a < users.length(); a++) {
                    this.buildUser(users.getJSONObject(a), handler);
                }
            }
            catch (JSONException ex) {
            }
            /*        } else if (objClass.is(OBCLASS_DEPARTMENT_NAME)) {
             String parent = null;
             if (options.getContainer() != null) {
             parent = options.getContainer().getUid().getUidValue();
             }
             NodeList depts = connection.getClient().listDepartments(query, options.getScope(), parent);
             log.info("{0} Departments Returned", depts.getLength());
             for (int a = 0; a < depts.getLength(); a++) {
             this.buildDepartment(depts.item(a), handler);
             }
             */
        } else {
            throw new IllegalArgumentException("Unsupported objectclass '" + objClass + "'");
        }

    }
    /*  public void executeQuery(ObjectClass objClass, String query, ResultsHandler handler, OperationOptions options) {
     if (ObjectClass.ACCOUNT.equals(objClass)) {
     NodeList users = connection.getClient().listUsers(query);
     log.info("{0} Users Returned", users.getLength());
     for (int a = 0; a < users.getLength(); a++) {
     this.buildUser(users.item(a), handler);
     }
     } else if (objClass.is(OBCLASS_DEPARTMENT_NAME)) {
     String parent = null;
     if (options.getContainer() != null) {
     parent = options.getContainer().getUid().getUidValue();
     }
     NodeList depts = connection.getClient().listDepartments(query, options.getScope(), parent);
     log.info("{0} Departments Returned", depts.getLength());
     for (int a = 0; a < depts.getLength(); a++) {
     this.buildDepartment(depts.item(a), handler);
     }
     } else {
     throw new IllegalArgumentException("Unsupported objectclass '" + objClass + "'");
     }



     throw new IllegalArgumentException("Unsupported objectclass '" + objClass + "'");

     }*/

    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objclass,
            Uid uid,
            Set<Attribute> replaceAttributes,
            OperationOptions options) {
         if (ObjectClass.ACCOUNT.equals(objclass)) {
            return connection.updateUser(uid.getUidValue(), replaceAttributes);
         } 
         /*else if (objclass.is(OBCLASS_DEPARTMENT_NAME)) {
         throw new IllegalArgumentException("Modifications to Departments not yet implimented");
         } */
         else {
         throw new IllegalArgumentException("Unsupported objectclass '" + objclass + "'");
         }
    }

    /**
     * {@inheritDoc}
     */
    public void test() {
        log.info("test connection");
        connection.testConnection();
    }

    protected void buildUser(JSONObject result, ResultsHandler handler) {
        try {
            ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
            builder.setObjectClass(ObjectClass.ACCOUNT);

            log.info("Building {1} results with Object: {0}", result.toString(), ObjectClass.ACCOUNT);

            if (result.getString("Type").equalsIgnoreCase("Replicon.Domain.User")) {
            } else {
                log.error("Result is not a Replicon.Domain.User object");
            }

            log.info("Person contains {0} attributes", result.length());

           

            JSONObject props = result.getJSONObject("Properties");

            log.info("Person contains {0} properties", props.length());

            
            builder.setUid(result.getString("Identity"));
            builder.setName(props.getString("LoginName"));
            
            
            Iterator itr = props.keys();
            while (itr.hasNext()) {
                String key = (String) itr.next();
                String value = null;
                try {
                value = props.getString(key);
                //log.info("Found Prop: {0} value: {1}", key, value);
                } catch (JSONException ex) {}
                if ((value != null) && (!(value.equalsIgnoreCase("null")))) {
                    // this is not great but it seems that JSON object strings break OpenIDM's parsing
                    if (!(value.contains("{"))){
                        log.info("Building Attr: {0} value: {1}", key, value);
                        builder.addAttribute(AttributeBuilder.build(key, value));
                    }
                }

            }
            
            ConnectorObject co = builder.build();
            
            log.info("Handling Connector Object Name: {0} Uid: {1} Class: {2}", co.getName(),co.getUid(),co.getObjectClass().getDisplayNameKey());
            
            //Build and call handler for the person
            handler.handle(co);
        }
        catch (JSONException ex) {
            log.error("Error parsing JSON reult");
        }
    }
}
