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
package org.identityconnectors.solaris;

import static org.identityconnectors.solaris.SolarisMessages.MSG_NOT_SUPPORTED_OBJECTCLASS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.Configuration;

public class OpCreateImpl extends AbstractOp {
    
    public OpCreateImpl(Configuration configuration, SolarisConnection connection, Log log) {
        super(configuration, connection, log);
    }
    
    Uid create(ObjectClass oclass, final Set<Attribute> attrs, final OperationOptions options) {
        if (!oclass.is(ObjectClass.ACCOUNT_NAME)) {
            throw new IllegalArgumentException(String.format(
                    MSG_NOT_SUPPORTED_OBJECTCLASS, ObjectClass.ACCOUNT_NAME));
        }
        
        // Read only list of attributes
        final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(
                AttributeUtil.toMap(attrs));

        final Name name = (Name) attrMap.get(Name.NAME);
        final String accountId = name.getNameValue();

        getLog().info("~~~~~~~ create(''{0}'') ~~~~~~~", accountId);
        
        /*
         * CREATE A NEW ACCOUNT
         */
        
        // USERADD accountId
        String command = String.format("useradd %s", accountId);
        //executeCommand(command);
        try {//CONNECTION
            getLog().info("useradd(''{0}'')", accountId);
            
            getConnection().resetStandardOutput();
            getConnection().send(command);
            getConnection().waitFor(getConfiguration().getRootShellPrompt());
            
        } catch (Exception ex) {
            getLog().error(ex.getMessage() + "\n");
            ex.printStackTrace();
        } //EOF CONNECTION
        
        /*
         * PASSWORD SET
         */
        final GuardedString password = SolarisHelper.getPasswordFromMap(attrMap);
        try {// CONNECTION
            getLog().info("passwd()");
            // TODO configurable source of password
            command = String.format("passwd -r files %s", accountId);
            getConnection().send(command);
            
            password.access(new GuardedString.Accessor() {
                public void access(char[] clearChars) {
                    String realPasswd = new String(clearChars);
                    try {
                        getConnection().waitFor("New Password:");
                        getConnection().send(realPasswd);
                        getConnection().waitFor("Re-enter new Password:");
                        getConnection().send(realPasswd);
                        getConnection().waitFor(String.format("passwd: password successfully changed for %s", accountId));
                    } catch (Exception ex) {
                        getLog().error(ex.getMessage() + "\n");
                        ex.printStackTrace();
                    }
                }
            });

        } catch (Exception ex) {
            getLog().error(ex.getMessage() + "\n");
            ex.printStackTrace();
        } // EOF CONNECTION
        // PASSWD password
        
        return new Uid(accountId);
    }
    
    private String executeCommand(String command) {
        return SolarisHelper.executeCommand(getConfiguration(), getConnection(), command);
    }
}
