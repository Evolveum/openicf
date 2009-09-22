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
import java.util.HashSet;
import java.util.List;
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
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.attr.ConnectorAttribute;
import org.identityconnectors.solaris.attr.GroupAttribute;
import org.identityconnectors.solaris.command.MatchBuilder;
import org.identityconnectors.solaris.command.closure.ClosureFactory;

import expect4j.Closure;
import expect4j.ExpectState;
import expect4j.matches.Match;

public class OpCreateImpl extends AbstractOp {
    
    final ObjectClass[] acceptOC = {ObjectClass.ACCOUNT, ObjectClass.GROUP};
    private final static Match[] errorsUseradd;
    static {
        MatchBuilder builder = new MatchBuilder();
        builder.addCaseInsensitiveRegExpMatch("invalid", ClosureFactory.newConnectorException("ERROR during execution of 'useradd' -- invalid command"));
        builder.addCaseInsensitiveRegExpMatch("ERROR", ClosureFactory.newConnectorException("ERROR during execution of 'useradd'"));
        builder.addCaseInsensitiveRegExpMatch("command not found", ClosureFactory.newConnectorException("'useradd' command is missing"));
        builder.addCaseInsensitiveRegExpMatch("not allowed to execute", ClosureFactory.newConnectorException("Not allowed to execute the 'useradd' command."));
        errorsUseradd = builder.build();
    }
    private final static Match[] errorsPasswd;
    static {
        MatchBuilder builder = new MatchBuilder();
        builder.addCaseInsensitiveRegExpMatch("Permission denied", ClosureFactory.newConnectorException("Permission denied when executing 'passwd'"));
        builder.addCaseInsensitiveRegExpMatch("command not found", ClosureFactory.newConnectorException("'passwd' command not found"));
        builder.addCaseInsensitiveRegExpMatch("not allowed to execute", ClosureFactory.newConnectorException("current user is not allowed to execute 'passwd' command"));
        errorsPasswd = builder.build();
    }
    
    public OpCreateImpl(Log log, SolarisConnector conn) {
        super(log, conn, OpCreateImpl.class);
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

        getLog().info("~~~~~~~ create(''{0}'') ~~~~~~~", accountId);
        
        if (accountExists(accountId)) {
            throw new ConnectorException("Account '" + accountId + "' already exists on the resource. The same user cannot be created multiple times.");
        }
        
        /*
         * CREATE A NEW ACCOUNT
         */
        final String commandSwitches = CommandUtil.prepareCommand(convertAttrsToPair(attrs, oclass));
        // USERADD accountId
        String command = getConnection().buildCommand("useradd", commandSwitches, accountId);
        
        Match[] matches = prepareMatches(getRootShellPrompt(), errorsUseradd);
        
        try {//CONNECTION
            getLog().info("useradd(''{0}'')", accountId);
            
            getConnection().send(command);
            getConnection().expect(matches);
        } catch (Exception ex) {
            getLog().error(ex, null);
        } //EOF CONNECTION
        
        /*
         * PASSWORD SET
         */
        
        final GuardedString password = SolarisUtil.getPasswordFromMap(attrMap);
        try {
            getLog().info("passwd()");
            // TODO configurable source of password (NIS and other resources?)
            command = String.format("passwd -r files %s", accountId);
            getConnection().send(command);
            
            matches = prepareMatches("New Password", errorsPasswd);
            getConnection().expect(matches);
            SolarisUtil.sendPassword(password, getConnection());
            
            matches = prepareMatches("Re-enter new Password:", errorsPasswd);
            getConnection().expect(matches);
            SolarisUtil.sendPassword(password, getConnection());
            
            //TODO what if something else happens?
            getConnection().waitFor(String.format("passwd: password successfully changed for %s", accountId));
        } catch (Exception ex) {
            getLog().error(ex, null);
        }
        
        /*
         * INACTIVE attribute
         */
        Attribute inactive = attrMap.get(AccountAttribute.INACTIVE);
        if (inactive != null) {
            //TODO
        }
        
        
        return new Uid(accountId);
    }

    static Set<NativePair> convertAttrsToPair(Set<Attribute> attrs, ObjectClass oclass) {
        Set<NativePair> set = new HashSet<NativePair>(attrs.size());
        for (Attribute attr : attrs) {
            final String attrName = attr.getName();
            ConnectorAttribute connAttr = (oclass.is(ObjectClass.ACCOUNT_NAME)) ? AccountAttribute.fromString(attrName) : GroupAttribute.fromString(attrName);

            if (connAttr == null)
                continue;

            List<Object> values = attr.getValue();
            String value = (values.size() > 0) ? (String) values.get(0) : null ;
            set.add(new NativePair(connAttr.getNative(), value));
        }
        return set;
    }

    /** checks if the account already exists on the resource. */
    private boolean accountExists(String name) {
        final boolean[] exists = new boolean[1];
        exists[0] = true;
        try {
            // FIXME find a more solid command that works for both NIS and normal passwords
            getConnection().send(getConnection().buildCommand(String.format("logins -l %s", name)));
            getConnection().expect(MatchBuilder.buildRegExpMatch(String.format("%s was not found", name), new Closure() {
                public void run(ExpectState state) throws Exception {
                    exists[0] = false; 
                }
            }));
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        
        return exists[0];
    }

    private Match[] prepareMatches(String string, Match[] commonErrMatches) {
        MatchBuilder builder = new MatchBuilder();
        builder.addNoActionMatch(string);
        builder.addMatches(commonErrMatches);
        
        return builder.build();
    }
}
