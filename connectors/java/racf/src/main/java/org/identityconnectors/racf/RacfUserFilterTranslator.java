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
package org.identityconnectors.racf;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;


/**
 * Create LDAP search Filter
 * <p>
 * Supported filters for "profileType=user,suffixDN" are:
 * <ul>
 *      <li>objectclass=*</li>
 *      <li>racfid=&lt;any_value&gt;</li>
 *      <li>racflnotesshortname=&lt;any_value&gt;</li>
 *      <li>racfndsusername=&lt;any_value&gt;</li>
 *      <li>racfomvsuid=&lt;number&gt;</li>
 *      <li>krbprincipalname=&lt;any_value&gt;</li>
 * </ul>
 * <p>
 * <p>
 * Complex search filters that 
 * include NOT, AND, OR, LE, or GE constructs are not supported.
 * <p>
 * The values for the racfid filter can include 
 * the wild cards supported by RACF. These wild cards are '*' which represents 
 * any number of characters, and '%' which represents one character. For example:
 * <pre>
 * (racfid=usr*)
 * </pre> 
 * searches for all the users whose names begin with usr.
 */
public class RacfUserFilterTranslator extends RacfLdapFilterTranslatorBase {

    protected boolean isFilterAttribute(Attribute attribute) {
        if (attribute.is(Uid.NAME))
            return true;
        if (attribute.is(Name.NAME))
            return true;
        if (attribute.is(RacfConstants.ATTR_LDAP_NDS_USER_NAME))
            return true;
        if (attribute.is(RacfConstants.ATTR_LDAP_LN_SHORT_NAME))
            return true;
        if (attribute.is(RacfConstants.ATTR_LDAP_OMVS_UID))
            return true;
        if (attribute.is(RacfConstants.ATTR_LDAP_KERB_NAME))
            return true;
        return false;
    }
    
    protected String getFilterAttributeName(Attribute attribute) {
        if (attribute.is(Name.NAME))
            return "racfid";
        else if (attribute.is(Uid.NAME))
            return "racfid";
        else
            return attribute.getName();
    }
}

