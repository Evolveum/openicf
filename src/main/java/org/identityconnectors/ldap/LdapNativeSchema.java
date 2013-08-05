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
package org.identityconnectors.ldap;

import java.util.Set;

/**
 * Provides an encapsulation of the LDAP server's native schema.
 */
public interface LdapNativeSchema {

    Set<String> getStructuralObjectClasses();

    Set<String> getRequiredAttributes(String ldapClass);

    Set<String> getOptionalAttributes(String ldapClass);

    /**
     * Returns the effective LDAP object classes that an entry of a given
     * object class would have, that is, including any superior object classes,
     * any superiors thereof, etc.
     */
    Set<String> getEffectiveObjectClasses(String ldapClass);

    LdapAttributeType getAttributeDescription(String ldapAttrName);
}
