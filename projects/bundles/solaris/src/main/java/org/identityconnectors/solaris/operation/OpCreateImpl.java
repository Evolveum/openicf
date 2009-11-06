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
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.operation.nis.CommonNIS;
import org.identityconnectors.solaris.operation.nis.OpCreateNISImpl;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class OpCreateImpl extends AbstractOp {

    private static final Log _log = Log.getLog(OpCreateImpl.class);
    
    final ObjectClass[] acceptOC = {ObjectClass.ACCOUNT, ObjectClass.GROUP};
    
    public OpCreateImpl(SolarisConnector conn) {
        super(conn);
    }

    /**
     * central method of Create operation. Internally it delegates the work to 
     * to NIS / Non-NIS (native) subordinates.
     */
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
        
        final SolarisEntry entry = SolarisUtil.forConnectorAttributeSet(name.getNameValue(), attrs);
        final GuardedString password = SolarisUtil.getPasswordFromMap(attrMap);
        
        if (SolarisUtil.isNis(getConnection())) {
            invokeNISCreate(entry, password);
        } else {
            invokeNativeCreate(entry, password);
        }
        
        return new Uid(accountId);
    }

    /**
     * NIS Create implementation.
     * 
     * Compare with Native create operation {@link OpCreateImpl#invokeNativeCreate(Set, Map, Name, String)}
     */
    private void invokeNISCreate(SolarisEntry entry, GuardedString password) {
        
        if (CommonNIS.isDefaultNisPwdDir(getConnection())) {
            invokeNativeCreate(entry, password);
            
            // The user has to be added to the NIS database
            /*
             * START SUDO
             */
            getConnection().doSudoStart();
            try {
                CommonNIS.addNISMake("passwd", getConnection());
            } finally {
                /*
                 * END SUDO
                 */
                getConnection().doSudoReset();
            }
        } else {
            OpCreateNISImpl.performNIS(entry, getConnection());
        }
    }
    

    /**
     * implementation of the Native Create operation.
     * 
     * Compare with other NIS implementation: {@see OpCreateImpl#invokeNISCreate(Set, Map, Name, String)}
     */
    private void invokeNativeCreate(SolarisEntry entry, GuardedString password) {
        /*
         * START SUDO
         */
        getConnection().doSudoStart();
        try {
            createImpl(entry, password);
        } finally {
            /*
             * END SUDO
             */
            getConnection().doSudoReset();
        }
    }

    /*
     * Note: do not invoke this from other then
     * OpCreateImpl.invokeNativeCreate(Set<Attribute>, Map<String, Attribute>,
     * Name, String) 
     * method
     */
    private void createImpl(SolarisEntry entry, GuardedString password) {
        getConnection().executeMutexAcquireScript();
        
        
        /*
         * CREATE A NEW ACCOUNT
         */
        _log.info("launching 'useradd' command (''{0}'')", entry.getName());
        try {
            CreateCommand.createUser(entry, getConnection());
        } finally {
            getConnection().executeMutexReleaseScript();
        }
        
        /*
         * PASSWORD SET
         */
        _log.info("launching 'passwd' command (''{0}'')", entry.getName());
        PasswdCommand.configureUserPassword(entry, password, getConnection());
        
        PasswdCommand.configurePasswordProperties(entry, getConnection());
    }
}
