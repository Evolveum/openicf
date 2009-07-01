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
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;


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
public class RacfUserFilterTranslator extends AbstractFilterTranslator<String> {
    @Override
    protected String createAndExpression(String leftExpression,
            String rightExpression) {
        // Although RACF does not support AND, we can use one of the filters,
        // since we just need to do partial filtering
        //
        if (leftExpression!=null)
            return leftExpression;
        else if (rightExpression!=null)
            return rightExpression;
        else
            return null;
    }
    @Override
    protected String createStartsWithExpression(StartsWithFilter filter,
            boolean not) {
        if (!not && isNameAttribute(filter.getAttribute()))
            return "("+getUserFilterAttributeName(filter.getAttribute())+"="+shorten(AttributeUtil.getAsStringValue(filter.getAttribute()), 7)+"*)";
        else
            return super.createStartsWithExpression(filter, not);
    }

    @Override
    protected String createEqualsExpression(EqualsFilter filter, boolean not) {
        if (!not && isNameAttribute(filter.getAttribute()))
            return "("+getUserFilterAttributeName(filter.getAttribute())+"="+getUidValue(filter.getAttribute())+")";
        else if (!not && isUserFilterAttribute(filter.getAttribute()))
            return "("+getUserFilterAttributeName(filter.getAttribute())+"="+AttributeUtil.getAsStringValue(filter.getAttribute())+")";
        else
            return super.createEqualsExpression(filter, not);
    }

    @Override
    protected String createEndsWithExpression(EndsWithFilter filter, boolean not) {
        if (!not && isNameAttribute(filter.getAttribute()))
            return "("+getUserFilterAttributeName(filter.getAttribute())+"=*"+shorten(AttributeUtil.getAsStringValue(filter.getAttribute()), -7)+")";
        else
            return super.createEndsWithExpression(filter, not);
    }

    @Override
    protected String createContainsExpression(ContainsFilter filter, boolean not) {
        if (!not && isNameAttribute(filter.getAttribute()))
            return "("+getUserFilterAttributeName(filter.getAttribute())+"=*"+shorten(AttributeUtil.getAsStringValue(filter.getAttribute()), 6)+"*)";
        else
            return super.createContainsExpression(filter, not);
    }

    private boolean isUserFilterAttribute(Attribute attribute) {
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
    

    private String shorten(String string, int length) {
        if (length > 0) {
            if (string.length() <= length)
                return string;
            else
                return string.substring(0, length);
        } else {
            if (string.length() <= -length)
                return string;
            else
                return string.substring(string.length()+length);
        }
    }
    

    private boolean isNameAttribute(Attribute attribute) {
        if (attribute.is(Uid.NAME))
            return true;
        if (attribute.is(Name.NAME))
            return true;
        return false;
    }
    
    private String getUidValue(Attribute attribute) {
        return RacfConnector.extractRacfIdFromLdapId(AttributeUtil.getStringValue(attribute));
    }

    private String getUserFilterAttributeName(Attribute attribute) {
        if (attribute.is(Name.NAME))
            return "racfid";
        else if (attribute.is(Uid.NAME))
            return "racfid";
        else
            return attribute.getName();
    }
}

