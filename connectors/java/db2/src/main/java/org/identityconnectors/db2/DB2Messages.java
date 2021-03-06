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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.db2;

abstract class DB2Messages {

    static final String VALIDATE_FAIL = "db2.validate.fail";
    static final String JDBC_DRIVER_CLASS_NOT_FOUND = "db2.jdbcDriverClassNotFound";
    static final String USERNAME_LONG = "db2.username.long";
    static final String USERNAME_CONTAINS_ILLEGAL_CHARACTERS =
            "db2.username.contains.illegal.characters";
    static final String USERNAME_IS_RESERVED_WORD = "db2.username.is.reserved.word";
    static final String USERNAME_HAS_INVALID_PREFIX = "db2.username.has.invalid.prefix";
    static final String UNSUPPORTED_OBJECT_CLASS = "db2.unsupported.object.class";
    static final String AUTHENTICATE_INVALID_CREDENTIALS = "db2.authenticate.invalid.credentials";
    static final String NAME_IS_NULL_OR_EMPTY = "db2.name.is.null.or.empty";
    static final String CREATE_OF_USER_FAILED = "db2.create.of.user.failed";
    static final String USER_ALREADY_EXISTS = "db2.user.already.exists";
    static final String USER_NOT_EXISTS = "db2.user.not.exists";
    static final String DELETE_OF_USER_FAILED = "db2.delete.of.user.failed";
    static final String NAME_IS_NOT_UPDATABLE = "db2.name.is.not.updatable";
    static final String UPDATE_OF_USER_FAILED = "db2.update.of.user.failed";
    static final String UPDATE_UID_CANNOT_BE_NULL_OR_EMPTY =
            "db2.update.uid.cannot.be.null.or.empty";
    static final String SEARCH_FAILED = "db2.search.failed";

    static final String DB2_CONNECTOR_DISPLAY = "db2.connector";
    static final String DB2_ADMINACCOUNT_DISPLAY = "db2.adminAccount.display";
    static final String DB2_ADMINACCOUNT_HELP = "db2.adminAccount.help";
    static final String DB2_ADMINPASSWORD_DISPLAY = "db2.adminPassword.display";
    static final String DB2_ADMINPASSWORD_HELP = "db2.adminPassword.help";
    static final String DB2_JDBCSUBPROTOCOL_DISPLAY = "db2.jdbcSubProtocol.display";
    static final String DB2_JDBCSUBPROTOCOL_HELP = "db2.jdbcSubProtocol.help";
    static final String DB2_DATABASENAME_DISPLAY = "db2.databaseName.display";
    static final String DB2_DATABASENAME_HELP = "db2.databaseName.help";
    static final String DB2_JDBCDRIVER_DISPLAY = "db2.jdbcDriver.display";
    static final String DB2_JDBCDRIVER_HELP = "db2.jdbcDriver.help";
    static final String DB2_HOST_DISPLAY = "db2.host.display";
    static final String DB2_HOST_HELP = "db2.host.help";
    static final String DB2_PORT_DISPLAY = "db2.port.display";
    static final String DB2_PORT_HELP = "db2.port.help";
    static final String DB2_URL_DISPLAY = "db2.url.display";
    static final String DB2_URL_HELP = "db2.url.help";
    static final String DB2_DATASOURCE_DISPLAY = "db2.dataSource.display";
    static final String DB2_DATASOURCE_HELP = "db2.dataSource.help";
    static final String DB2_DSJNDIENV_DISPLAY = "db2.dsJNDIEnv.display";
    static final String DB2_DSJNDIENV_HELP = "db2.dsJNDIEnv.help";
    static final String DB2_REPLACEALLGRANTSONUPDATE_DISPLAY =
            "db2.replaceAllGrantsOnUpdate.display";
    static final String DB2_REPLACEALLGRANTSONUPDATE_HELP = "db2.replaceAllGrantsOnUpdate.help";
}
