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
 * Portions Copyrighted 2013-2014 ForgeRock AS
 */
package org.identityconnectors.ldap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorSecurityException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.PasswordExpiredException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.ldap.LdapConnection.AuthenticationResult;
import org.identityconnectors.ldap.LdapConnection.AuthenticationResultType;
import org.identityconnectors.ldap.search.LdapSearches;

public class LdapAuthenticate {

    private final LdapConnection conn;
    private final ObjectClass oclass;
    private final String username;
    private final OperationOptions options;

    public LdapAuthenticate(LdapConnection conn, ObjectClass objectClass, String username, OperationOptions options) {
        this.conn = conn;
        this.oclass = objectClass;
        this.username = username;
        this.options = options;
    }

    public Uid authenticate(GuardedString password) {
        ConnectorObject authnObject = getObjectToAuthenticate();
        if (authnObject == null) {
            throw new InvalidCredentialException(conn.format("cannotResolveUsername", null, username));
        }
        AuthenticationResult authnResult = null;
        String entryDN = authnObject.getAttributeByName("entryDN").getValue().get(0).toString();
        authnResult = conn.authenticate(entryDN, password);

        if (authnResult == null ) {
            throw new InvalidCredentialException(conn.format("authenticationFailed", null, username));
        }
        try {
            authnResult.propagate();
        } catch (PasswordExpiredException e) {
            e.initUid(authnObject.getUid());
            throw e;
        }
        // AuthenticationResult did not throw an exception, so this authentication was successful.
        return authnObject.getUid();
    }

    public Uid resolveUsername() {
        ConnectorObject authnObject = getObjectToAuthenticate();
        if (authnObject == null) {
            throw new InvalidCredentialException(conn.format("cannotResolveUsername", null, username));
        }
        return authnObject.getUid();
    }
    
    public String getDn() {
        ConnectorObject authnObject = getObjectToAuthenticate();
        if (authnObject == null) {
            throw new InvalidCredentialException(conn.format("cannotResolveUsername", null, username));
        }
        return authnObject.getName().getNameValue();
        
    }

    private ConnectorObject getObjectToAuthenticate() {
        List<String> userNameAttrs = getUserNameAttributes();
        Map<String, ConnectorObject> entryDN2Object = new HashMap<String, ConnectorObject>();
        for (String baseContext : conn.getConfiguration().getBaseContexts()) {
            for (String userNameAttr : userNameAttrs) {
                Attribute attr = AttributeBuilder.build(userNameAttr, username);
                for (ConnectorObject object : LdapSearches.findObjects(conn, oclass, baseContext, attr, "entryDN")) {
                    String entryDN = object.getAttributeByName("entryDN").getValue().get(0).toString();
                    entryDN2Object.put(entryDN, object);
                }
                // If we found more than one authentication candidates, no need to continue
                if (entryDN2Object.size() > 1) {
                    throw new ConnectorSecurityException(conn.format("moreThanOneEntryMatched", null, username));
                }
            }
        }
        if (!entryDN2Object.isEmpty()) {
            return entryDN2Object.values().iterator().next();
        }
        return null;
    }

    private List<String> getUserNameAttributes() {
        String[] result = LdapConstants.getLdapUidAttributes(options);
        if (result != null && result.length > 0) {
            return Arrays.asList(result);
        }
        return conn.getSchemaMapping().getUserNameLdapAttributes(oclass);
    }

    private static boolean isSuccess(AuthenticationResult authResult) {
        // We consider PASSWORD_EXPIRED to be a success, because it means the credentials were right.
        AuthenticationResultType type = authResult.getType();
        return type.equals(AuthenticationResultType.SUCCESS) || type.equals(AuthenticationResultType.PASSWORD_EXPIRED);
    }
}
