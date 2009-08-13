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


public abstract class RacfLdapFilterTranslatorBase extends AbstractFilterTranslator<String> {
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
            return "("+getFilterAttributeName(filter.getAttribute())+"="+shorten(AttributeUtil.getAsStringValue(filter.getAttribute()), 7)+"*)";
        else
            return super.createStartsWithExpression(filter, not);
    }

    @Override
    protected String createEqualsExpression(EqualsFilter filter, boolean not) {
        if (!not && isNameAttribute(filter.getAttribute()))
            return "("+getFilterAttributeName(filter.getAttribute())+"="+getUidValue(filter.getAttribute())+")";
        else if (!not && isFilterAttribute(filter.getAttribute()))
            return "("+getFilterAttributeName(filter.getAttribute())+"="+AttributeUtil.getAsStringValue(filter.getAttribute())+")";
        else
            return super.createEqualsExpression(filter, not);
    }

    @Override
    protected String createEndsWithExpression(EndsWithFilter filter, boolean not) {
        if (!not && isNameAttribute(filter.getAttribute()))
            return "("+getFilterAttributeName(filter.getAttribute())+"=*"+shorten(AttributeUtil.getAsStringValue(filter.getAttribute()), -7)+")";
        else
            return super.createEndsWithExpression(filter, not);
    }

    @Override
    protected String createContainsExpression(ContainsFilter filter, boolean not) {
        if (!not && isNameAttribute(filter.getAttribute()))
            return "("+getFilterAttributeName(filter.getAttribute())+"=*"+shorten(AttributeUtil.getAsStringValue(filter.getAttribute()), 6)+"*)";
        else
            return super.createContainsExpression(filter, not);
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
    

    protected boolean isNameAttribute(Attribute attribute) {
        if (attribute.is(Uid.NAME))
            return true;
        if (attribute.is(Name.NAME))
            return true;
        return false;
    }
    
    private String getUidValue(Attribute attribute) {
        return RacfConnector.extractRacfIdFromLdapId(AttributeUtil.getStringValue(attribute));
    }

    protected abstract boolean isFilterAttribute(Attribute attribute);
    protected abstract String getFilterAttributeName(Attribute attribute);
}

