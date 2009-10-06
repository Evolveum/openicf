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
package org.identityconnectors.solaris.operation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class OpCreateImpl extends AbstractOp {
    private static final Log _log = Log.getLog(OpCreateImpl.class);
    
    final ObjectClass[] acceptOC = {ObjectClass.ACCOUNT, ObjectClass.GROUP};
    
    public OpCreateImpl(SolarisConnector conn) {
        super(conn);
    }

    public Uid create(ObjectClass oclass, final Set<Attribute> attrs, final OperationOptions options) {
        SolarisUtil.controlObjectClassValidity(oclass, acceptOC, getClass());
        
        if (oclass.is(ObjectClass.GROUP_NAME)) {
            // TODO
            throw new UnsupportedOperationException();
        }
        
        // Read only list of attributes
        final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));

        final Name name = (Name) attrMap.get(Name.NAME);
        final String accountId = name.getNameValue();

        _log.info("~~~~~~~ create(''{0}'') ~~~~~~~", accountId);
        
        if (accountExists(accountId)) {
            throw new ConnectorException("Account '" + accountId + "' already exists on the resource. The same user cannot be created multiple times.");
        }
        
        /*
         * START SUDO
         */
        getConnection().doSudoStart();
        try {
            createImpl(attrs, attrMap, name, accountId);
        } finally {
            /*
             * END SUDO
             */
            getConnection().doSudoReset();
        }
        return new Uid(accountId);
    }

    private void createImpl(final Set<Attribute> attrs,
            final Map<String, Attribute> attrMap, final Name name,
            final String accountId) {
        /*
         * First acquire the "mutex" for uid creation
         */
        getConnection().executeMutexAcquireScript();
        
        
        /*
         * CREATE A NEW ACCOUNT
         */
        _log.info("launching 'useradd' command (''{0}'')", accountId);
        final SolarisEntry entry = SolarisUtil.forConnectorAttributeSet(name.getNameValue(), attrs);
        try {
            CreateCommand.createUser(entry, getConnection());
        } finally {
            /*
             * Release the uid "mutex"
             */
            getConnection().executeMutexReleaseScript();
        }
        
        /*
         * PASSWORD SET
         */
        _log.info("launching 'passwd' command (''{0}'')", accountId);
        GuardedString password = SolarisUtil.getPasswordFromMap(attrMap);
        PasswdCommand.configureUserPassword(entry, password, getConnection());
        
        PasswdCommand.configurePasswordProperties(entry, getConnection());
    }

    /** checks if the account already exists on the resource. */
    private boolean accountExists(String name) {
        try {
            // FIXME find a more solid command that works for both NIS and normal passwords
            final String out = getConnection().executeCommand(getConnection().buildCommand(String.format("logins -l %s", name)));
            if (!out.contains(String.format("%s was not found", name))) {
                return true;
            } 
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        
        return false;
    }
}
