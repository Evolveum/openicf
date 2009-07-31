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
package org.identityconnectors.webtimesheet;

import java.util.*;

import org.identityconnectors.common.security.*;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.common.logging.Log;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Main implementation of the WebTimeSheet Connector
 * 
 * @author Robert Jackson - <a href='http://www.nulli.com'>Nulli Secundus Inc.</a>
 * @version 1.0
 * @since 1.0
 */
@ConnectorClass(displayNameKey = "WebTimeSheet",
configurationClass = WebTimeSheetConfiguration.class)
public class WebTimeSheetConnector implements PoolableConnector, CreateOp, SchemaOp, TestOp, DeleteOp, UpdateOp, SearchOp<String> {

    public static final java.lang.String ATTR_LAST_NAME = "LastName";
    public static final java.lang.String ATTR_FIRST_NAME = "FirstName";
    public static final java.lang.String ATTR_LOGIN_NAME = "LoginName";
    public static final java.lang.String ATTR_ID = "Id";
    public static final java.lang.String ATTR_EMPLOYEE_ID = "EmployeeId";
    public static final java.lang.String ATTR_INTERNAL_EMAIL = "InternalEmail";
    public static final java.lang.String ATTR_PARENT_ID = "ParentDepartmentId";
    public static final java.lang.String ATTR_DEPARTMENT = "DepartmentId";
    public static final java.lang.String OBCLASS_DEPARTMENT_NAME = "Department";
    /**
     * Setup logging for the {@link WebTimeSheetConnector}.
     */
    private static final Log log = Log.getLog(WebTimeSheetConnector.class);
    /**
     * Place holder for the Connection created in the init method
     */
    private WebTimeSheetConnection connection;
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
        this.connection = new WebTimeSheetConnection(this.config);
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
        connection.test();
    }

    /******************
     * SPI Operations
     * 
     * Implement the following operations using the contract and
     * description found in the Javadoc for these methods.
     ******************/
    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objClass, final Set<Attribute> attrs, final OperationOptions options) {
        AttributesAccessor a = new AttributesAccessor(attrs);
        if (ObjectClass.ACCOUNT.equals(objClass)) {
            return connection.getClient().createUser(
                    a.findString(ATTR_FIRST_NAME),
                    a.findString(ATTR_LAST_NAME),
                    a.findString(ATTR_LOGIN_NAME),
                    getPlainPassword(a.getPassword()),
                    a.findString(ATTR_DEPARTMENT),
                    a.findString(ATTR_INTERNAL_EMAIL),
                    a.findString(ATTR_EMPLOYEE_ID),
                    a.getEnabled(true));
        } else if (objClass.is(OBCLASS_DEPARTMENT_NAME)) {
            throw new IllegalArgumentException("Creation of Departments not yet implimented");
        } else {
            throw new IllegalArgumentException("Unsupported objectclass '" + objClass + "'");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objClass, final Uid uid, final OperationOptions options) {
        if (ObjectClass.ACCOUNT.equals(objClass)) {
            connection.getClient().deleteUser(uid.getUidValue());
        } else if (objClass.is(OBCLASS_DEPARTMENT_NAME)) {
            throw new IllegalArgumentException("Deletions of Departments not yet implimented");
        } else {
            throw new IllegalArgumentException("Unsupported objectclass '" + objClass + "'");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        if (config == null) {
            throw new IllegalStateException("Configuration object has not been set.");
        }
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

        // Operational attributes
        //
        attributes.add(OperationalAttributeInfos.PASSWORD);
        attributes.add(OperationalAttributeInfos.ENABLE);

        schemaBuilder.defineObjectClass(ObjectClass.ACCOUNT_NAME, attributes);


        // Department objects
        //
        attributes = new HashSet<AttributeInfo>();
        aib = new AttributeInfoBuilder();
        aib.setUpdateable(false);
        aib.setName(Name.NAME);
        attributes.add(aib.build());
        aib.setName(ATTR_ID);
        attributes.add(aib.build());

        aib.setName(ATTR_ID);
        attributes.add(aib.build());

        aib.setName(ATTR_PARENT_ID);
        attributes.add(aib.build());

        ObjectClassInfoBuilder oib = new ObjectClassInfoBuilder();
        oib.addAllAttributeInfo(attributes);
        oib.setContainer(true);
        oib.setType(OBCLASS_DEPARTMENT_NAME);



        // TODO: define supported Op's for Department in schema
        schemaBuilder.defineObjectClass(oib.build());



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

    }

    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objclass,
            Uid uid,
            Set<Attribute> replaceAttributes,
            OperationOptions options) {
        AttributesAccessor a = new AttributesAccessor(replaceAttributes);
        if (ObjectClass.ACCOUNT.equals(objclass)) {
            return connection.getClient().updateUser(uid,
                    a.findString(ATTR_FIRST_NAME),
                    a.findString(ATTR_LAST_NAME),
                    a.findString(ATTR_LOGIN_NAME),
                    getPlainPassword(a.getPassword()),
                    a.findString(ATTR_DEPARTMENT),
                    a.findString(ATTR_INTERNAL_EMAIL),
                    a.findString(ATTR_EMPLOYEE_ID),
                    a.getEnabled(true));
        } else if (objclass.is(OBCLASS_DEPARTMENT_NAME)) {
            throw new IllegalArgumentException("Modifications to Departments not yet implimented");
        } else {
            throw new IllegalArgumentException("Unsupported objectclass '" + objclass + "'");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void test() {
        log.info("test connection");
        connection.test();
    }

    protected void buildUser(Node personElement, ResultsHandler handler) {

        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(ObjectClass.ACCOUNT);

        NodeList attrList = personElement.getChildNodes();

        log.info("Person contains {0} attributes", attrList.getLength());

        for (int a = 0; a < attrList.getLength(); a++) {
            Node attrElement = attrList.item(a);
            if (attrElement.getNodeType() == Node.ELEMENT_NODE) {
                log.info("Attr: {0} = {1}", attrElement.getNodeName(), attrElement.getTextContent());
                if (attrElement.getNodeName().equalsIgnoreCase(ATTR_ID)) {
                    //Handle Unique ID
                    log.info("Found Unique Attr");
                    Uid uid = new Uid(attrElement.getTextContent());
                    builder.setUid(uid);
                    builder.setName(uid.getUidValue());
                    builder.addAttribute(AttributeBuilder.build(attrElement.getNodeName(), attrElement.getTextContent()));
                } else if (attrElement.getNodeName().equalsIgnoreCase("Enabled")) {
                    builder.addAttribute(AttributeBuilder.build("__ENABLED__", attrElement.getTextContent()));
                
                } else {
                    builder.addAttribute(AttributeBuilder.build(attrElement.getNodeName(), attrElement.getTextContent()));
                }
                builder.addAttribute(AttributeBuilder.buildEnabled(true));
            }
        }

        //Build and call handler for the person
        handler.handle(builder.build());

    }

    protected void buildDepartment(Node deptElement, ResultsHandler handler) {

        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(new ObjectClass(OBCLASS_DEPARTMENT_NAME));

        NodeList attrList = deptElement.getChildNodes();

        log.info("Department contains {0} attributes", attrList.getLength());

        for (int a = 0; a < attrList.getLength(); a++) {
            Node attrElement = attrList.item(a);
            if (attrElement.getNodeType() == Node.ELEMENT_NODE) {
                log.info("Attr: {0} = {1}", attrElement.getNodeName(), attrElement.getTextContent());
                if (attrElement.getNodeName().equalsIgnoreCase(ATTR_ID)) {
                    //Handle Unique ID
                    log.info("Found Unique Attr");
                    Uid uid = new Uid(attrElement.getTextContent());
                    builder.setUid(uid);
                    builder.addAttribute(AttributeBuilder.build(attrElement.getNodeName(), attrElement.getTextContent()));
                } else if (attrElement.getNodeName().equalsIgnoreCase("NAME")) {
                    builder.setName(attrElement.getTextContent());
                } else {
                    builder.addAttribute(AttributeBuilder.build(attrElement.getNodeName(), attrElement.getTextContent()));
                }
                builder.addAttribute(AttributeBuilder.buildEnabled(true));
            }
        }

        //Build and call handler for the department
        handler.handle(builder.build());

    }

    private String getPlainPassword(GuardedString password) {
        if (password == null) {
            return null;
        }
        final StringBuffer buf = new StringBuffer();
        password.access(new GuardedString.Accessor() {
            public void access(char[] clearChars) {
                buf.append(clearChars);
            }
        });
        return buf.toString();
    }
}
